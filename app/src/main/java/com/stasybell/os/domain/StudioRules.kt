package com.stasybell.os.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object StudioRules {
    val STUDIO_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    private val RU_LOCALE = Locale.forLanguageTag("ru-RU")

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", RU_LOCALE)
    private val DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", RU_LOCALE)
    private val TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", RU_LOCALE)
    private val MONTH_YEAR_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL yyyy", RU_LOCALE)

    const val SLOT_STEP_MINUTES = 15
    const val PUBLIC_BOOKING_HORIZON_DAYS = 30
    const val SESSION_TTL_DAYS = 30L

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMATTER)

    fun formatTime(instant: Instant): String =
        instant.atZone(STUDIO_ZONE).format(TIME_FORMATTER)

    fun formatDateTime(instant: Instant): String =
        instant.atZone(STUDIO_ZONE).format(DATE_TIME_FORMATTER)

    fun formatMinuteOfDay(minute: Int): String {
        val h = (minute / 60).coerceIn(0, 23)
        val m = (minute % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        val raw = yearMonth.atDay(1).format(MONTH_YEAR_FORMATTER)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(RU_LOCALE) else it.toString() }
    }

    fun toLocalDate(instant: Instant): LocalDate =
        instant.atZone(STUDIO_ZONE).toLocalDate()

    fun toYearMonth(instant: Instant): YearMonth =
        YearMonth.from(instant.atZone(STUDIO_ZONE))

    fun toInstant(date: LocalDate, minuteOfDay: Int): Instant {
        val hours = minuteOfDay / 60
        val minutes = minuteOfDay % 60
        return ZonedDateTime.of(date, LocalTime.of(hours, minutes), STUDIO_ZONE).toInstant()
    }

    /**
     * Formats integer kopecks to Russian ruble string (e.g., 350000 -> "3 500 ₽", 270050 -> "2 700,50 ₽").
     */
    fun formatRubles(kopecks: Long): String {
        val negative = kopecks < 0
        val absKopecks = kotlin.math.abs(kopecks)
        val rubles = absKopecks / 100
        val remKopecks = absKopecks % 100
        val grouped = rubles.toString().reversed().chunked(3).joinToString(" ").reversed()
        val sign = if (negative) "−" else ""
        return if (remKopecks == 0L) {
            "$sign$grouped ₽"
        } else {
            "$sign$grouped,%02d ₽".format(remKopecks)
        }
    }

    /**
     * Parses user input like "3500", "3 500,50", "800.25" into exact integer kopecks.
     */
    fun parseRublesToKopecks(input: String): Long? {
        val cleaned = input.trim().replace(" ", "").replace("₽", "").replace(",", ".")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            val kopecks = bd.movePointRight(2).longValueExact()
            if (kopecks <= 0L) null else kopecks
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats integer milliunits (1000 = 1.000 unit) with up to 3 decimal places.
     */
    fun formatQuantity(milliunits: Long, unit: MaterialUnit): String {
        val negative = milliunits < 0
        val absVal = kotlin.math.abs(milliunits)
        val whole = absVal / 1000
        val frac = absVal % 1000
        val sign = if (negative) "−" else ""
        val numberStr = if (frac == 0L) {
            whole.toString()
        } else {
            val fracStr = "%03d".format(frac).trimEnd('0')
            "$whole,$fracStr"
        }
        return "$sign$numberStr ${unit.labelRu}"
    }

    /**
     * Parses quantity like "15,5", "100", "0.125" into integer milliunits (precision 0.001).
     */
    fun parseQuantityToMilliunits(input: String): Long? {
        val cleaned = input.trim().replace(" ", "").replace(",", ".")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd.scale() > 3) return null
            val milli = bd.setScale(3, RoundingMode.UNNECESSARY).movePointRight(3).longValueExact()
            if (milli <= 0L) null else milli
        } catch (_: Exception) {
            null
        }
    }

    fun rangesOverlap(startA: Instant, endA: Instant, startB: Instant, endB: Instant): Boolean {
        return startA.isBefore(endB) && startB.isBefore(endA)
    }

    /**
     * Validates that a proposed [startsAt, occupiedUntil) interval fits inside working windows
     * (for appointments) and does not collide with any active existing entry.
     */
    fun validateSlotPlacement(
        kind: AppointmentKind,
        startsAt: Instant,
        durationMinutes: Int,
        bufferMinutes: Int,
        workingWindows: List<WorkingWindow>,
        existingEntries: List<CalendarEntry>,
        excludeEntryId: String? = null
    ) {
        if (durationMinutes <= 0) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Длительность должна быть больше 0 минут")
        }
        if (bufferMinutes < 0) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Буфер не может быть отрицательным")
        }
        val startZdt = startsAt.atZone(STUDIO_ZONE)
        val occupiedUntil = startsAt.plusSeconds((durationMinutes + bufferMinutes) * 60L)
        val endZdt = occupiedUntil.atZone(STUDIO_ZONE)

        // Prevent crossing midnight without explicit consent
        if (startZdt.toLocalDate() != endZdt.toLocalDate() &&
            !(endZdt.toLocalTime() == LocalTime.MIDNIGHT && endZdt.toLocalDate() == startZdt.toLocalDate().plusDays(1))
        ) {
            throw DomainException(
                ErrorCode.OUTSIDE_WORKING_WINDOW,
                "Запись не может переходить через полночь"
            )
        }

        if (kind == AppointmentKind.APPOINTMENT) {
            val day = startZdt.toLocalDate()
            val dayWindows = workingWindows.filter { it.date == day }
            if (dayWindows.isEmpty()) {
                throw DomainException(ErrorCode.WORKING_DAY_CLOSED, "Рабочий день закрыт")
            }
            val startMin = startZdt.hour * 60 + startZdt.minute
            val endMin = startMin + durationMinutes + bufferMinutes
            val fitsWindow = dayWindows.any { w ->
                startMin >= w.startMinuteOfDay && endMin <= w.endMinuteOfDay
            }
            if (!fitsWindow) {
                throw DomainException(
                    ErrorCode.OUTSIDE_WORKING_WINDOW,
                    "Процедура с буфером выходит за пределы рабочего окна"
                )
            }
        }

        val conflict = existingEntries.any { existing ->
            existing.id != excludeEntryId &&
                existing.deletedAt == null &&
                existing.status.blocksSlot &&
                rangesOverlap(startsAt, occupiedUntil, existing.startsAt, existing.occupiedUntil)
        }
        if (conflict) {
            throw DomainException(ErrorCode.SLOT_CONFLICT, "Время уже занято")
        }
    }

    /**
     * Validates non-overlapping working windows and guarantees existing active appointments are not orphaned.
     */
    fun validateWorkingWindowsUpdate(
        date: LocalDate,
        proposedWindows: List<WorkingWindow>,
        existingEntriesOnDate: List<CalendarEntry>
    ) {
        val sorted = proposedWindows.sortedBy { it.startMinuteOfDay }
        for (i in sorted.indices) {
            val w = sorted[i]
            if (w.startMinuteOfDay < 0 || w.endMinuteOfDay > 24 * 60 || w.startMinuteOfDay >= w.endMinuteOfDay) {
                throw DomainException(ErrorCode.INVALID_WORKING_WINDOW, "Некорректный интервал рабочего времени")
            }
            if (i > 0 && sorted[i - 1].endMinuteOfDay > w.startMinuteOfDay) {
                throw DomainException(
                    ErrorCode.INVALID_WORKING_WINDOW,
                    "Рабочие интервалы не должны пересекаться"
                )
            }
        }

        val activeAppointments = existingEntriesOnDate.filter {
            it.deletedAt == null &&
                it.kind == AppointmentKind.APPOINTMENT &&
                it.status.blocksSlot &&
                toLocalDate(it.startsAt) == date
        }
        for (appt in activeAppointments) {
            val zdt = appt.startsAt.atZone(STUDIO_ZONE)
            val startMin = zdt.hour * 60 + zdt.minute
            val endMin = startMin + appt.durationMinutes + appt.bufferMinutes
            val stillCovered = sorted.any { w ->
                startMin >= w.startMinuteOfDay && endMin <= w.endMinuteOfDay
            }
            if (!stillCovered) {
                throw DomainException(
                    ErrorCode.WINDOW_OVERLAPS_APPOINTMENT,
                    "Нельзя закрыть рабочее окно: в это время уже есть активная запись (${formatTime(appt.startsAt)})"
                )
            }
        }
    }

    /**
     * Computes available 15-minute start slots on a given date for a service duration + buffer.
     */
    fun computeAvailableStartMinutes(
        date: LocalDate,
        durationMinutes: Int,
        bufferMinutes: Int,
        workingWindows: List<WorkingWindow>,
        existingEntries: List<CalendarEntry>,
        excludeEntryId: String? = null
    ): List<Int> {
        val totalSpan = durationMinutes + bufferMinutes
        val dayWindows = workingWindows.filter { it.date == date }.sortedBy { it.startMinuteOfDay }
        if (dayWindows.isEmpty() || totalSpan <= 0) return emptyList()

        val activeBlocking = existingEntries.filter {
            it.id != excludeEntryId && it.deletedAt == null && it.status.blocksSlot
        }
        val result = mutableListOf<Int>()
        for (w in dayWindows) {
            var cursor = ((w.startMinuteOfDay + SLOT_STEP_MINUTES - 1) / SLOT_STEP_MINUTES) * SLOT_STEP_MINUTES
            while (cursor + totalSpan <= w.endMinuteOfDay) {
                val startInstant = toInstant(date, cursor)
                val endInstant = startInstant.plusSeconds(totalSpan * 60L)
                val blocked = activeBlocking.any { existing ->
                    rangesOverlap(startInstant, endInstant, existing.startsAt, existing.occupiedUntil)
                }
                if (!blocked) {
                    result.add(cursor)
                }
                cursor += SLOT_STEP_MINUTES
            }
        }
        return result
    }

    /**
     * Returns utilization percentage (0..100), or null when the day is off / has no working windows.
     */
    fun computeUtilizationPercent(
        workingWindows: List<WorkingWindow>,
        entries: List<CalendarEntry>
    ): Int? {
        val availableMinutes = workingWindows.sumOf { (it.endMinuteOfDay - it.startMinuteOfDay).coerceAtLeast(0) }
        if (availableMinutes <= 0) return null
        val occupiedMinutes = entries
            .filter { it.deletedAt == null && it.kind == AppointmentKind.APPOINTMENT && it.status.blocksSlot }
            .sumOf { it.durationMinutes + it.bufferMinutes }
        return ((occupiedMinutes * 100.0) / availableMinutes).toInt().coerceIn(0, 100)
    }

    /**
     * Computes actual cash ledger summary from manual FinanceTransactions only (never from visits or archive).
     */
    fun computeFinanceSummary(
        yearMonth: YearMonth,
        transactions: List<FinanceTransaction>,
        closures: List<MonthClosure>
    ): FinanceSummary {
        val monthTx = transactions.filter { toYearMonth(it.occurredAt) == yearMonth }
        val income = monthTx.filter { it.kind == FinanceKind.INCOME }.sumOf { it.amountKopecks }
        val expense = monthTx.filter { it.kind == FinanceKind.EXPENSE }.sumOf { it.amountKopecks }
        val balance = income - expense

        val monthClosures = closures
            .filter { it.year == yearMonth.year && it.month == yearMonth.monthValue }
            .sortedByDescending { it.version }
        val latestClosure = monthClosures.firstOrNull()
        val isClosed = latestClosure?.isClosed == true

        val expenseByCategory = FinanceCategory.forKind(FinanceKind.EXPENSE).associateWith { cat ->
            monthTx.filter { it.kind == FinanceKind.EXPENSE && it.category == cat }.sumOf { it.amountKopecks }
        }

        val weeklyIncome = MutableList(5) { 0L }
        val weeklyExpense = MutableList(5) { 0L }
        for (tx in monthTx) {
            val dayOfMonth = toLocalDate(tx.occurredAt).dayOfMonth
            val weekIndex = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
            if (tx.kind == FinanceKind.INCOME) {
                weeklyIncome[weekIndex] += tx.amountKopecks
            } else {
                weeklyExpense[weekIndex] += tx.amountKopecks
            }
        }

        return FinanceSummary(
            yearMonth = yearMonth,
            incomeKopecks = income,
            expenseKopecks = expense,
            balanceKopecks = balance,
            isClosed = isClosed,
            latestClosure = latestClosure,
            closures = monthClosures,
            expenseByCategory = expenseByCategory,
            weeklyIncomeKopecks = weeklyIncome,
            weeklyExpenseKopecks = weeklyExpense
        )
    }

    /**
     * Validates remote server URL security: HTTPS required in production/release.
     * Local HTTP is permitted only in explicit debug mode for localhost/emulator/trusted LAN.
     */
    fun validateServerUrl(rawUrl: String, allowDebugHttp: Boolean): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите адрес сервера")
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("https://")) {
            return trimmed
        }
        if (allowDebugHttp && (lower.startsWith("http://127.0.0.1") ||
                lower.startsWith("http://10.0.2.2") ||
                lower.startsWith("http://localhost"))
        ) {
            return trimmed
        }
        throw DomainException(
            ErrorCode.HTTPS_REQUIRED,
            "Для рабочего сервера требуется защищённое соединение HTTPS"
        )
    }

    fun validateStockMovement(currentStockMilliunits: Long, deltaMilliunits: Long): Long {
        if (deltaMilliunits == 0L) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите ненулевое количество")
        }
        val next = currentStockMilliunits + deltaMilliunits
        if (next < 0L) {
            throw DomainException(ErrorCode.INSUFFICIENT_STOCK, "На складе недостаточно материала")
        }
        return next
    }
}
