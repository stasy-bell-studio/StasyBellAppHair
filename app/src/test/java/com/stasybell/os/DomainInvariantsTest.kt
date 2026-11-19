package com.stasybell.os

import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.DomainException
import com.stasybell.os.domain.ErrorCode
import com.stasybell.os.domain.MaterialUnit
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.security.LoginRateLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pure JVM tests validating core domain invariants (06_TESTS_AND_ACCEPTANCE.md groups A-E).
 */
class DomainInvariantsTest {

    @Test
    fun `A01 rate limiter blocks after 5 failed attempts and resets on success`() {
        val limiter = LoginRateLimiter(maxFailures = 5, cooldownMs = 60_000L)
        repeat(4) {
            assertNull(limiter.checkAllowed(1000L))
            limiter.recordFailure(1000L)
        }
        assertNull(limiter.checkAllowed(1000L))
        limiter.recordFailure(1000L)
        val wait = limiter.checkAllowed(2000L)
        assertNotNull(wait)
        assertTrue(wait!! >= 59L)

        limiter.recordSuccess()
        assertNull(limiter.checkAllowed(3000L))
    }

    @Test
    fun `A05 release rejects insecure HTTP URLs and requires HTTPS`() {
        val ex = assertThrows(DomainException::class.java) {
            StudioRules.validateServerUrl("http://192.168.10.126:8000", allowDebugHttp = false)
        }
        assertEquals(ErrorCode.HTTPS_REQUIRED, ex.code)

        val valid = StudioRules.validateServerUrl("https://api.stasybell.ru/", allowDebugHttp = false)
        assertEquals("https://api.stasybell.ru", valid)

        val debugLocal = StudioRules.validateServerUrl("http://10.0.2.2:8000", allowDebugHttp = true)
        assertEquals("http://10.0.2.2:8000", debugLocal)
    }

    @Test
    fun `B04 and B06 buffer overlap and working window boundary are strictly rejected`() {
        val date = LocalDate.of(2026, 12, 31)
        val windows = listOf(
            WorkingWindow("w1", date, 10 * 60, 14 * 60),
            WorkingWindow("w2", date, 14 * 60 + 30, 18 * 60)
        )
        val start10 = StudioRules.toInstant(date, 10 * 60)
        val existing = listOf(
            CalendarEntry(
                id = "e1",
                kind = AppointmentKind.APPOINTMENT,
                clientId = "c1",
                clientNameSnapshot = "Анна",
                serviceId = "s1",
                serviceNameSnapshot = "Кератин",
                source = AppointmentSource.PHONE,
                status = AppointmentStatus.CONFIRMED,
                startsAt = start10,
                endsAt = start10.plusSeconds(120 * 60L),
                occupiedUntil = start10.plusSeconds(135 * 60L), // 12:15 with 15-min buffer
                durationMinutes = 120,
                bufferMinutes = 15,
                quotedPriceKopecks = 620_000,
                note = ""
            )
        )

        // Starting at 12:00 collides with 15-min buffer (12:00..12:15) -> 409 SLOT_CONFLICT
        val conflict = assertThrows(DomainException::class.java) {
            StudioRules.validateSlotPlacement(
                kind = AppointmentKind.APPOINTMENT,
                startsAt = StudioRules.toInstant(date, 12 * 60),
                durationMinutes = 60,
                bufferMinutes = 15,
                workingWindows = windows,
                existingEntries = existing
            )
        }
        assertEquals(ErrorCode.SLOT_CONFLICT, conflict.code)

        // Starting at 17:00 for 60 min + 15 min buffer ends at 18:15 > 18:00 -> OUTSIDE_WORKING_WINDOW
        val overflow = assertThrows(DomainException::class.java) {
            StudioRules.validateSlotPlacement(
                kind = AppointmentKind.APPOINTMENT,
                startsAt = StudioRules.toInstant(date, 17 * 60),
                durationMinutes = 60,
                bufferMinutes = 15,
                workingWindows = windows,
                existingEntries = existing
            )
        }
        assertEquals(ErrorCode.OUTSIDE_WORKING_WINDOW, overflow.code)
    }

    @Test
    fun `B09 leap day 29_02_2024 and Dec-Jan transition preserve Europe Moscow local date`() {
        val leap = LocalDate.of(2024, 2, 29)
        assertEquals("29.02.2024", StudioRules.formatDate(leap))

        val dec31Late = StudioRules.toInstant(LocalDate.of(2026, 12, 31), 23 * 60 + 30)
        assertEquals(LocalDate.of(2026, 12, 31), StudioRules.toLocalDate(dec31Late))

        val jan1Early = StudioRules.toInstant(LocalDate.of(2027, 1, 1), 0 * 60 + 15)
        assertEquals(LocalDate.of(2027, 1, 1), StudioRules.toLocalDate(jan1Early))
    }

    @Test
    fun `C02 kopeck integer parsing and formatting stay exact`() {
        assertEquals(350_000L, StudioRules.parseRublesToKopecks("3500"))
        assertEquals(80_050L, StudioRules.parseRublesToKopecks("800,50"))
        assertEquals("3 500 ₽", StudioRules.formatRubles(350_000L))
        assertEquals("2 700 ₽", StudioRules.formatRubles(350_000L - 80_000L))
        assertEquals("2 700,50 ₽", StudioRules.formatRubles(270_050L))
    }

    @Test
    fun `D01 and D03 stock milliunits preserve 0_001 precision and reject negative result`() {
        val receipt = StudioRules.parseQuantityToMilliunits("100")!!
        val deduction = StudioRules.parseQuantityToMilliunits("15,5")!!
        val remaining = StudioRules.validateStockMovement(receipt, -deduction)
        assertEquals(84_500L, remaining)
        assertEquals("84,5 г", StudioRules.formatQuantity(remaining, MaterialUnit.GRAM))

        val ex = assertThrows(DomainException::class.java) {
            StudioRules.validateStockMovement(remaining, -100_000L)
        }
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, ex.code)
    }

    @Test
    fun `Today utilization returns null on day off rather than misleading 0 or 100`() {
        assertNull(StudioRules.computeUtilizationPercent(emptyList(), emptyList()))
    }
}
