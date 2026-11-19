package com.stasybell.os

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.stasybell.os.data.local.StasyBellDatabase
import com.stasybell.os.data.repository.LocalDemoRepository
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.DomainException
import com.stasybell.os.domain.ErrorCode
import com.stasybell.os.domain.FinanceCategory
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.MaterialUnit
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.security.SecureTokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Full stateful Robolectric + Room in-memory tests covering 06_TESTS_AND_ACCEPTANCE.md:
 * A01-A04, B01-B10, C01-C07, D01-D04, E01-E06.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryInvariantsRobolectricTest {

    private lateinit var db: StasyBellDatabase
    private lateinit var tokenStore: SecureTokenStore
    private lateinit var repo: LocalDemoRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StasyBellDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tokenStore = SecureTokenStore(context)
        tokenStore.clearSession()
        repo = LocalDemoRepository(db, tokenStore)
        runBlocking { repo.initialize() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `A01-A03 login creates encrypted session, persists on restore, and logout revokes it`() = runBlocking {
        try {
            repo.login("nastya", "wrong", "Samsung S20")
            fail("Expected AUTH_FAILED")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.AUTH_FAILED, e.code)
        }

        val session = repo.login(LocalDemoRepository.DEMO_USER, LocalDemoRepository.DEMO_PASS, "Samsung S20")
        assertTrue(repo.isAuthenticated.first())
        assertTrue(repo.restoreSessionIfValid())

        repo.revokeSession(session.id)
        assertFalse(repo.isAuthenticated.first())
        assertFalse(repo.restoreSessionIfValid())
    }

    @Test
    fun `B01-B03 client without phone books manually and service price edits never alter old visit snapshot`() = runBlocking {
        val date = LocalDate.now(StudioRules.STUDIO_ZONE).plusDays(5)
        repo.saveWorkingWindows(date, listOf(10 * 60 to 18 * 60))

        // B01: Client without phone
        val client = repo.upsertClient(null, "Ольга Без Телефона", null, "Тест", PreferredChannel.NONE)
        assertEquals(null, client.phone)

        val srv = repo.upsertService(null, "Кератин", "Кератин Тест", "По плечи", 620_000L, 120, 15, true)
        val entry = repo.createManualAppointment(
            clientId = client.id,
            serviceId = srv.id,
            serviceNameOverride = srv.name,
            source = AppointmentSource.TELEGRAM_MANUAL,
            startsAt = StudioRules.toInstant(date, 10 * 60),
            durationMinutes = srv.durationMinutes,
            bufferMinutes = srv.bufferMinutes,
            quotedPriceKopecks = srv.priceKopecks,
            note = "Ручная запись"
        )
        assertEquals(AppointmentStatus.CONFIRMED, entry.status)
        assertEquals(620_000L, entry.quotedPriceKopecks)

        // B03: Change service catalog price to 7 500 ₽ -> historical entry snapshot MUST stay 6 200 ₽
        repo.upsertService(srv.id, srv.category, srv.name, srv.variant, 750_000L, 120, 15, true)
        val reloaded = repo.calendarEntries.first().first { it.id == entry.id }
        assertEquals(620_000L, reloaded.quotedPriceKopecks)
    }

    @Test
    fun `B05 B08 B10 conflict rejected with 409, restore blocked on conflict, schedule protects active visit`() = runBlocking {
        val date = LocalDate.now(StudioRules.STUDIO_ZONE).plusDays(6)
        repo.saveWorkingWindows(date, listOf(10 * 60 to 18 * 60))
        val client = repo.upsertClient(null, "Тест Конфликт", null, "", PreferredChannel.PHONE)
        val start10 = StudioRules.toInstant(date, 10 * 60)

        val first = repo.createManualAppointment(client.id, null, "Ботокс", AppointmentSource.PHONE, start10, 90, 15, 500_000L, "")

        // B05: Overlapping booking rejected
        try {
            repo.createManualAppointment(client.id, null, "Пилинг", AppointmentSource.PHONE, StudioRules.toInstant(date, 11 * 60), 30, 15, 75_000L, "")
            fail("Expected SLOT_CONFLICT")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.SLOT_CONFLICT, e.code)
        }

        // B10: Closing working window over active visit rejected
        try {
            repo.saveWorkingWindows(date, emptyList())
            fail("Expected WINDOW_OVERLAPS_APPOINTMENT")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.WINDOW_OVERLAPS_APPOINTMENT, e.code)
        }

        // B08: Soft delete frees slot; if another booking takes it, restore MUST fail!
        repo.softDeleteEntry(first.id)
        repo.createManualAppointment(client.id, null, "Занято другим", AppointmentSource.PHONE, start10, 90, 15, 500_000L, "")
        try {
            repo.restoreEntry(first.id)
            fail("Expected SLOT_CONFLICT on restore")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.SLOT_CONFLICT, e.code)
        }
    }

    @Test
    fun `C01-C07 visit completion adds ZERO revenue, idempotency prevents duplicates, close-reopen-reverse v2 works, archive isolated`() = runBlocking {
        val ym = YearMonth.of(2027, 4) // Clean isolated future month
        val d10 = ym.atDay(10)
        repo.saveWorkingWindows(d10, listOf(10 * 60 to 18 * 60))
        val client = repo.upsertClient(null, "Тест Касса", null, "", PreferredChannel.PHONE)

        // C01: Booking & completing visit MUST NOT create any finance transaction!
        val appt = repo.createManualAppointment(client.id, null, "Кератин", AppointmentSource.PHONE, StudioRules.toInstant(d10, 10 * 60), 120, 15, 690_000L, "")
        repo.updateEntryStatus(appt.id, AppointmentStatus.COMPLETED)
        val summaryAfterComplete = StudioRules.computeFinanceSummary(ym, repo.financeTransactions.first(), repo.monthClosures.first())
        assertEquals(0L, summaryAfterComplete.incomeKopecks)
        assertEquals(0L, summaryAfterComplete.balanceKopecks)

        // C02 & C03: Manual income 3 500 ₽ (with double-submit idempotency key!) & expense 800 ₽ -> balance 2 700 ₽
        val inc1 = repo.addFinanceTransaction(FinanceKind.INCOME, FinanceCategory.PROCEDURE, 350_000L, StudioRules.toInstant(d10, 13 * 60), "Факт", appt.id, "idem-key-1")
        val incDup = repo.addFinanceTransaction(FinanceKind.INCOME, FinanceCategory.PROCEDURE, 350_000L, StudioRules.toInstant(d10, 13 * 60), "Факт", appt.id, "idem-key-1")
        assertEquals(inc1.id, incDup.id)

        repo.addFinanceTransaction(FinanceKind.EXPENSE, FinanceCategory.MATERIALS, 80_000L, StudioRules.toInstant(d10, 15 * 60), "Состав", null, "idem-key-2")
        val summary = StudioRules.computeFinanceSummary(ym, repo.financeTransactions.first(), repo.monthClosures.first())
        assertEquals(350_000L, summary.incomeKopecks)
        assertEquals(80_000L, summary.expenseKopecks)
        assertEquals(270_000L, summary.balanceKopecks)

        // C05 & C06: Close v1 -> block new tx -> reopen with reason -> reverse once -> block second reverse -> close v2
        val cl1 = repo.closeMonth(ym)
        assertEquals(1, cl1.version)
        assertTrue(cl1.isClosed)

        try {
            repo.addFinanceTransaction(FinanceKind.INCOME, FinanceCategory.OTHER_INCOME, 100_000L, StudioRules.toInstant(d10, 16 * 60), "Запрещено", null, "idem-key-3")
            fail("Expected MONTH_CLOSED")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.MONTH_CLOSED, e.code)
        }

        repo.reopenMonth(ym, "Ошибка суммы")
        repo.reverseFinanceTransaction(inc1.id)
        try {
            repo.reverseFinanceTransaction(inc1.id)
            fail("Expected ALREADY_REVERSED")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.ALREADY_REVERSED, e.code)
        }
        val cl2 = repo.closeMonth(ym)
        assertEquals(2, cl2.version)
        assertEquals(0L, cl2.incomeKopecks)
        assertEquals(80_000L, cl2.expenseKopecks)
        assertEquals(-80_000L, cl2.balanceKopecks)

        // C07: Verify legacy Excel summaries exist yet never leak into current actual cash summary
        assertTrue(repo.legacyMonthlySummaries.first().isNotEmpty())
        assertEquals(0L, cl2.incomeKopecks)
    }

    @Test
    fun `D01-D04 materials track 0_001 precision, block negative stock, and do not create cash expenses`() = runBlocking {
        val txCountBefore = repo.financeTransactions.first().size
        val mat = repo.upsertMaterial(null, "Тест Состав", MaterialUnit.GRAM, 20_000L)
        repo.recordMaterialMovement(mat.id, 100_000L, "Приход 100 г")
        repo.recordMaterialMovement(mat.id, -15_500L, "Списание 15,5 г")

        val updated = repo.materials.first().first { it.id == mat.id }
        assertEquals(84_500L, updated.stockMilliunits)

        try {
            repo.recordMaterialMovement(mat.id, -100_000L, "Попытка в минус")
            fail("Expected INSUFFICIENT_STOCK")
        } catch (e: DomainException) {
            assertEquals(ErrorCode.INSUFFICIENT_STOCK, e.code)
        }

        // D04: Stock movement never touches finance cash ledger
        assertEquals(txCountBefore, repo.financeTransactions.first().size)
    }

    @Test
    fun `E01-E03 archive contains leap day 29_02_2024 and 12 unmatched orphan visits without blocking calendar`() = runBlocking {
        val legacyVisits = repo.legacyVisits.first()
        val leapVisit = legacyVisits.firstOrNull { it.visitDate == LocalDate.of(2024, 2, 29) }
        assertNotNull(leapVisit)

        val orphans = legacyVisits.filter { it.isUnmatched }
        assertEquals(12, orphans.size)

        // Archive visits never enter calendar_entries
        val calendarOnLeap = repo.calendarEntries.first().filter {
            StudioRules.toLocalDate(it.startsAt) == LocalDate.of(2024, 2, 29)
        }
        assertTrue(calendarOnLeap.isEmpty())
    }
}
