package com.stasybell.os.data.local

import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.FinanceCategory
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.MaterialUnit
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.domain.StudioRules
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Populates synthetic Demo data ONLY when Room is empty.
 * No real personal data, phones, or production financial figures are stored here.
 * Prices reflect the editable template from the public website (marked Demo).
 */
object DemoSeedData {

    suspend fun seedIfEmpty(dao: StasyBellDao, today: LocalDate = LocalDate.now(StudioRules.STUDIO_ZONE)) {
        if (dao.getServicesOnce().isNotEmpty()) return

        val nowMs = Instant.now().toEpochMilli()

        // 1. Services seeded from the current website price list (editable Demo template)
        val services = listOf(
            ServiceEntity("srv_kb_1", "KB-SHOULDER", "Кератин / Ботокс", "Кератин / Ботокс", "По плечи", 620_000, 150, 15, true),
            ServiceEntity("srv_kb_2", "KB-BELOW", "Кератин / Ботокс", "Кератин / Ботокс", "Ниже плеч", 650_000, 165, 15, true),
            ServiceEntity("srv_kb_3", "KB-BLADE", "Кератин / Ботокс", "Кератин / Ботокс", "По лопатки", 690_000, 180, 15, true),
            ServiceEntity("srv_kb_4", "KB-WAIST", "Кератин / Ботокс", "Кератин / Ботокс", "По талию", 720_000, 195, 15, true),
            ServiceEntity("srv_kb_5", "KB-LOWER", "Кератин / Ботокс", "Кератин / Ботокс", "По поясницу", 770_000, 210, 15, true),
            ServiceEntity("srv_combo_1", "CMB-CARE", "Комбо-процедуры", "Уходовая стрижка + пилинг + Здоровье", "Комплекс", 800_000, 150, 15, true),
            ServiceEntity("srv_combo_2", "CMB-VOL", "Комбо-процедуры", "Здоровье + пилинг + прикорневой объём", "Комплекс", 850_000, 180, 15, true),
            ServiceEntity("srv_hlt_1", "HLT-SHOULDER", "Здоровье для волос", "Здоровье для волос", "По плечи", 370_000, 90, 15, true),
            ServiceEntity("srv_hlt_2", "HLT-BLADE", "Здоровье для волос", "Здоровье для волос", "По лопатки", 390_000, 105, 15, true),
            ServiceEntity("srv_hlt_3", "HLT-WAIST", "Здоровье для волос", "Здоровье для волос", "По талию", 420_000, 120, 15, true),
            ServiceEntity("srv_add_1", "ADD-PEEL", "Дополнительные услуги", "Пилинг кожи головы", "Стандарт", 75_000, 30, 15, true),
            ServiceEntity("srv_add_2", "ADD-ROOT", "Дополнительные услуги", "Прикорневой объём на кератине", "От 7 см", 550_000, 120, 15, true)
        )
        dao.upsertServices(services)

        // 2. Synthetic demo clients (including a client WITHOUT phone to verify phone-optional flow)
        val clients = listOf(
            ClientEntity("cl_1", "Анна (Демо)", "+7 900 000-01-01", "Тонкие волосы, предпочитает ботокс", PreferredChannel.TELEGRAM.code, null, null, nowMs - 86_400_000L * 40),
            ClientEntity("cl_2", "Мария (Демо)", "+7 900 000-01-02", "Густые волосы по лопатки (+10%)", PreferredChannel.VK.code, null, null, nowMs - 86_400_000L * 25),
            ClientEntity("cl_3", "Елена (Демо, без тел.)", null, "Запись из личных сообщений без телефона", PreferredChannel.NONE.code, null, null, nowMs - 86_400_000L * 10),
            ClientEntity("cl_4", "София (Демо)", "+7 900 000-01-04", "Регулярный уход раз в 3 месяца", PreferredChannel.PHONE.code, null, null, nowMs - 86_400_000L * 5)
        )
        dao.upsertClients(clients)

        // 3. Working windows for the next 30 days (and past 7 days): 10:00-14:00 & 14:30-20:00 (with break), Sunday off
        val windows = mutableListOf<WorkingWindowEntity>()
        for (offset in -7..30) {
            val d = today.plusDays(offset.toLong())
            if (d.dayOfWeek.value != 7 || offset == 0) { // Ensure today is always open for demo preview
                val iso = d.toString()
                windows.add(WorkingWindowEntity("ww_${iso}_1", iso, 10 * 60, 14 * 60))
                windows.add(WorkingWindowEntity("ww_${iso}_2", iso, 14 * 60 + 30, 20 * 60))
            }
        }
        dao.insertWorkingWindows(windows)

        // 4. Demo calendar entries (today + tomorrow + past completed)
        val todayStart1 = StudioRules.toInstant(today, 10 * 60)
        val todayStart2 = StudioRules.toInstant(today, 14 * 60 + 30)
        val tomorrowStart = StudioRules.toInstant(today.plusDays(1), 11 * 60)
        val yesterdayStart = StudioRules.toInstant(today.minusDays(1), 10 * 60)

        val entries = listOf(
            CalendarEntryEntity(
                id = "ent_1",
                kindCode = AppointmentKind.APPOINTMENT.code,
                clientId = "cl_1",
                clientNameSnapshot = "Анна (Демо)",
                serviceId = "srv_kb_3",
                serviceNameSnapshot = "Кератин / Ботокс · По лопатки",
                sourceCode = AppointmentSource.TELEGRAM_MANUAL.code,
                statusCode = AppointmentStatus.CONFIRMED.code,
                startsAtMs = todayStart1.toEpochMilli(),
                endsAtMs = todayStart1.plusSeconds(180 * 60L).toEpochMilli(),
                occupiedUntilMs = todayStart1.plusSeconds(195 * 60L).toEpochMilli(),
                durationMinutes = 180,
                bufferMinutes = 15,
                quotedPriceKopecks = 690_000,
                note = "Демо-запись на сегодня",
                deletedAtMs = null
            ),
            CalendarEntryEntity(
                id = "ent_2",
                kindCode = AppointmentKind.APPOINTMENT.code,
                clientId = "cl_3",
                clientNameSnapshot = "Елена (Демо, без тел.)",
                serviceId = "srv_hlt_2",
                serviceNameSnapshot = "Здоровье для волос · По лопатки",
                sourceCode = AppointmentSource.VK_MANUAL.code,
                statusCode = AppointmentStatus.CONFIRMED.code,
                startsAtMs = todayStart2.toEpochMilli(),
                endsAtMs = todayStart2.plusSeconds(105 * 60L).toEpochMilli(),
                occupiedUntilMs = todayStart2.plusSeconds(120 * 60L).toEpochMilli(),
                durationMinutes = 105,
                bufferMinutes = 15,
                quotedPriceKopecks = 390_000,
                note = "Создана вручную без номера телефона",
                deletedAtMs = null
            ),
            CalendarEntryEntity(
                id = "ent_3",
                kindCode = AppointmentKind.APPOINTMENT.code,
                clientId = "cl_2",
                clientNameSnapshot = "Мария (Демо)",
                serviceId = "srv_combo_1",
                serviceNameSnapshot = "Уходовая стрижка + пилинг + Здоровье",
                sourceCode = AppointmentSource.PHONE.code,
                statusCode = AppointmentStatus.CONFIRMED.code,
                startsAtMs = tomorrowStart.toEpochMilli(),
                endsAtMs = tomorrowStart.plusSeconds(150 * 60L).toEpochMilli(),
                occupiedUntilMs = tomorrowStart.plusSeconds(165 * 60L).toEpochMilli(),
                durationMinutes = 150,
                bufferMinutes = 15,
                quotedPriceKopecks = 800_000,
                note = "Позвонила по телефону",
                deletedAtMs = null
            ),
            CalendarEntryEntity(
                id = "ent_4",
                kindCode = AppointmentKind.APPOINTMENT.code,
                clientId = "cl_4",
                clientNameSnapshot = "София (Демо)",
                serviceId = "srv_kb_1",
                serviceNameSnapshot = "Кератин / Ботокс · По плечи",
                sourceCode = AppointmentSource.PHONE.code,
                statusCode = AppointmentStatus.COMPLETED.code,
                startsAtMs = yesterdayStart.toEpochMilli(),
                endsAtMs = yesterdayStart.plusSeconds(150 * 60L).toEpochMilli(),
                occupiedUntilMs = yesterdayStart.plusSeconds(165 * 60L).toEpochMilli(),
                durationMinutes = 150,
                bufferMinutes = 15,
                quotedPriceKopecks = 620_000,
                note = "Процедура выполнена (оплата вносится вручную)",
                deletedAtMs = null
            )
        )
        dao.upsertCalendarEntries(entries)

        dao.insertCalendarEvents(
            entries.map { e ->
                CalendarEventEntity(
                    id = "ev_${e.id}_created",
                    entryId = e.id,
                    action = "created",
                    actor = "Настя",
                    summary = "Создана запись (${AppointmentSource.fromCode(e.sourceCode).labelRu})",
                    occurredAtMs = nowMs - 3_600_000L
                )
            }
        )

        // 5. Manual actual cash transactions (current month + closed previous month)
        val prevMonth = YearMonth.from(today).minusMonths(1)
        val prevMidMs = StudioRules.toInstant(prevMonth.atDay(15), 16 * 60).toEpochMilli()
        val prevRentMs = StudioRules.toInstant(prevMonth.atDay(5), 11 * 60).toEpochMilli()
        val txTodayMs = StudioRules.toInstant(today, 12 * 60).toEpochMilli()
        val txYesterdayMs = StudioRules.toInstant(today.minusDays(1), 13 * 60).toEpochMilli()

        dao.upsertTransactions(
            listOf(
                FinanceTransactionEntity(
                    id = "tx_1",
                    entryId = "ent_4",
                    kindCode = FinanceKind.INCOME.code,
                    categoryCode = FinanceCategory.PROCEDURE.code,
                    amountKopecks = 620_000,
                    occurredAtMs = txYesterdayMs,
                    note = "Фактическая оплата визита Софии (Демо)",
                    correctionOfId = null,
                    idempotencyKey = "seed_tx_1"
                ),
                FinanceTransactionEntity(
                    id = "tx_2",
                    entryId = null,
                    kindCode = FinanceKind.EXPENSE.code,
                    categoryCode = FinanceCategory.MATERIALS.code,
                    amountKopecks = 240_000,
                    occurredAtMs = txTodayMs - 7_200_000L,
                    note = "Закупка составов (отдельно от склада)",
                    correctionOfId = null,
                    idempotencyKey = "seed_tx_2"
                ),
                FinanceTransactionEntity(
                    id = "tx_prev_1",
                    entryId = null,
                    kindCode = FinanceKind.INCOME.code,
                    categoryCode = FinanceCategory.PROCEDURE.code,
                    amountKopecks = 4_850_000,
                    occurredAtMs = prevMidMs,
                    note = "Фактические поступления прошлого месяца (Демо)",
                    correctionOfId = null,
                    idempotencyKey = "seed_tx_prev_1"
                ),
                FinanceTransactionEntity(
                    id = "tx_prev_2",
                    entryId = null,
                    kindCode = FinanceKind.EXPENSE.code,
                    categoryCode = FinanceCategory.RENT.code,
                    amountKopecks = 1_500_000,
                    occurredAtMs = prevRentMs,
                    note = "Аренда студии (Демо)",
                    correctionOfId = null,
                    idempotencyKey = "seed_tx_prev_2"
                )
            )
        )

        dao.upsertMonthClosure(
            MonthClosureEntity(
                id = "cl_${prevMonth.year}_${prevMonth.monthValue}_v1",
                year = prevMonth.year,
                month = prevMonth.monthValue,
                version = 1,
                incomeKopecks = 4_850_000,
                expenseKopecks = 1_500_000,
                balanceKopecks = 3_350_000,
                isClosed = true,
                closedAtMs = StudioRules.toInstant(today.withDayOfMonth(1), 10 * 60).toEpochMilli(),
                closedBy = "Настя",
                reopenedAtMs = null,
                reopenReason = null
            )
        )

        // 6. Materials inventory (milliunits: 1000 = 1.000)
        dao.upsertMaterials(
            listOf(
                MaterialEntity("mat_1", "Кератин премиум состав (Демо)", MaterialUnit.GRAM.code, 420_500, 150_000),
                MaterialEntity("mat_2", "Ботокс-концентрат (Демо)", MaterialUnit.MILLILITER.code, 85_000, 100_000),
                MaterialEntity("mat_3", "Шампунь глубокой очистки (Демо)", MaterialUnit.MILLILITER.code, 650_000, 200_000),
                MaterialEntity("mat_4", "Перчатки нитриловые (Демо)", MaterialUnit.PIECE.code, 42_000, 20_000)
            )
        )
        dao.insertMovements(
            listOf(
                MaterialMovementEntity("mov_1", "mat_1", 500_000, "Приход партии (Демо)", "Настя", nowMs - 86_400_000L * 5),
                MaterialMovementEntity("mov_2", "mat_1", -79_500, "Расход на процедуры (Демо)", "Настя", nowMs - 86_400_000L * 2),
                MaterialMovementEntity("mov_3", "mat_2", 120_000, "Приход партии (Демо)", "Настя", nowMs - 86_400_000L * 7),
                MaterialMovementEntity("mov_4", "mat_2", -35_000, "Списание на визит (Демо)", "Настя", nowMs - 86_400_000L * 1)
            )
        )

        // 7. Strictly isolated Legacy Excel Archive (synthetic demo rows, including leap day 29.02.2024 + 12 orphan visits)
        val legacyVisits = mutableListOf(
            LegacyVisitEntity("leg_v_1", "xlsx:row:14", "2024-02-29", "Кератин (архив Excel)", 650_000, "demo_archive.xlsx", "Визиты", 14, "cl_1", "Анна (Демо)", false),
            LegacyVisitEntity("leg_v_2", "xlsx:row:32", "2024-05-18", "Ботокс (архив Excel)", 620_000, "demo_archive.xlsx", "Визиты", 32, "cl_1", "Анна (Демо)", false),
            LegacyVisitEntity("leg_v_3", "xlsx:row:47", "2024-08-03", "Здоровье для волос (архив)", 390_000, "demo_archive.xlsx", "Визиты", 47, "cl_2", "Мария (Демо)", false)
        )
        // 12 synthetic orphan/unmatched visits for manual reconciliation queue
        for (i in 1..12) {
            val day = (i * 2).coerceAtMost(28)
            legacyVisits.add(
                LegacyVisitEntity(
                    id = "leg_orphan_$i",
                    sourceRef = "xlsx:orphan:$i",
                    visitDateIso = "2024-03-%02d".format(day),
                    serviceTitle = "Архивная процедура #$i (требует сверки)",
                    amountOriginKopecks = 500_000L + i * 25_000L,
                    sourceFile = "demo_archive.xlsx",
                    sourceSheet = "Неразобрано",
                    sourceRow = 100 + i,
                    clientId = null,
                    clientRawName = "Неопознанный клиент #$i",
                    isUnmatched = true
                )
            )
        }
        dao.upsertLegacyVisits(legacyVisits)

        dao.upsertLegacyMonthlySummaries(
            listOf(
                LegacyMonthlySummaryEntity("leg_m_1", "2024-02", 6_450_000, 1_420_000, 11, 14, "Демо-архив (неподтверждённая касса)"),
                LegacyMonthlySummaryEntity("leg_m_2", "2024-03", 7_120_000, 1_610_000, 13, 16, "Демо-архив (неподтверждённая касса)"),
                LegacyMonthlySummaryEntity("leg_m_3", "2024-04", 6_890_000, 1_530_000, 12, 15, "Демо-архив (неподтверждённая касса)")
            )
        )

        dao.upsertLegacyCatalogItems(
            listOf(
                LegacyCatalogItemEntity("leg_c_1", "cat:1", "Архивная формула кератина #1", "Исторический каталог", 580_000),
                LegacyCatalogItemEntity("leg_c_2", "cat:2", "Архивный ботокс-уход #2", "Исторический каталог", 520_000)
            )
        )
    }
}
