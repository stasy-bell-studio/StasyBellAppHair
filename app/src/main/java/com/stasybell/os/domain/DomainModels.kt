package com.stasybell.os.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class DataMode(val labelRu: String) {
    DEMO_ROOM("Демо (локальная БД Room)"),
    REMOTE_FASTAPI("Сервер FastAPI (HTTPS)")
}

enum class AppointmentKind(val code: String, val labelRu: String) {
    APPOINTMENT("appointment", "Запись"),
    BLOCK("block", "Блокировка");

    companion object {
        fun fromCode(code: String): AppointmentKind =
            entries.firstOrNull { it.code == code } ?: APPOINTMENT
    }
}

enum class AppointmentStatus(val code: String, val labelRu: String, val blocksSlot: Boolean) {
    PENDING_CHANNEL("pending_channel", "Ожидает подтверждения", true),
    CONFIRMED("confirmed", "Подтверждена", true),
    EXPIRED("expired", "Истекла", false),
    CANCELLED("cancelled", "Отменена", false),
    COMPLETED("completed", "Завершена", true),
    NO_SHOW("no_show", "Неявка", false),
    BLOCKED("blocked", "Заблокировано", true);

    companion object {
        fun fromCode(code: String): AppointmentStatus =
            entries.firstOrNull { it.code == code } ?: CONFIRMED
    }
}

enum class AppointmentSource(val code: String, val labelRu: String) {
    WEBSITE("website", "Сайт"),
    PHONE("phone", "Телефон"),
    TELEGRAM_MANUAL("telegram_manual", "Telegram (вручную)"),
    VK_MANUAL("vk_manual", "VK (вручную)"),
    OTHER_MANUAL("other_manual", "Другое (вручную)");

    companion object {
        fun fromCode(code: String): AppointmentSource =
            entries.firstOrNull { it.code == code } ?: PHONE
    }
}

enum class PreferredChannel(val code: String, val labelRu: String) {
    NONE("none", "Не указан"),
    TELEGRAM("telegram", "Telegram"),
    VK("vk", "ВКонтакте"),
    PHONE("phone", "Телефон");

    companion object {
        fun fromCode(code: String): PreferredChannel =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class FinanceKind(val code: String, val labelRu: String) {
    INCOME("income", "Доход"),
    EXPENSE("expense", "Расход");

    companion object {
        fun fromCode(code: String): FinanceKind =
            entries.firstOrNull { it.code == code } ?: INCOME
    }
}

enum class FinanceCategory(
    val code: String,
    val labelRu: String,
    val kind: FinanceKind
) {
    PROCEDURE("procedure", "Процедуры", FinanceKind.INCOME),
    OTHER_INCOME("other", "Прочие поступления", FinanceKind.INCOME),
    MATERIALS("materials", "Материалы", FinanceKind.EXPENSE),
    RENT("rent", "Аренда", FinanceKind.EXPENSE),
    TAXES("taxes", "Налоги", FinanceKind.EXPENSE),
    ADVERTISING("advertising", "Реклама", FinanceKind.EXPENSE),
    OTHER_EXPENSE("other", "Прочие расходы", FinanceKind.EXPENSE);

    companion object {
        fun forKind(kind: FinanceKind): List<FinanceCategory> =
            entries.filter { it.kind == kind }

        fun fromCode(kind: FinanceKind, code: String): FinanceCategory =
            entries.firstOrNull { it.kind == kind && it.code == code }
                ?: if (kind == FinanceKind.INCOME) PROCEDURE else OTHER_EXPENSE
    }
}

enum class MaterialUnit(val code: String, val labelRu: String) {
    GRAM("g", "г"),
    MILLILITER("ml", "мл"),
    PIECE("pcs", "шт");

    companion object {
        fun fromCode(code: String): MaterialUnit =
            entries.firstOrNull { it.code == code } ?: GRAM
    }
}

enum class ErrorCode {
    SLOT_CONFLICT,
    WORKING_DAY_CLOSED,
    OUTSIDE_WORKING_WINDOW,
    WINDOW_OVERLAPS_APPOINTMENT,
    INVALID_WORKING_WINDOW,
    MONTH_CLOSED,
    ALREADY_REVERSED,
    INSUFFICIENT_STOCK,
    INVALID_INPUT,
    AUTH_FAILED,
    RATE_LIMITED,
    HTTPS_REQUIRED,
    NETWORK_ERROR,
    NOT_FOUND
}

class DomainException(
    val code: ErrorCode,
    override val message: String
) : Exception(message)

data class OwnerSession(
    val id: String,
    val username: String,
    val deviceName: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant,
    val revoked: Boolean,
    val isCurrent: Boolean = false
)

data class Client(
    val id: String,
    val name: String,
    val phone: String?,
    val notes: String,
    val preferredChannel: PreferredChannel,
    val telegramChatId: String?,
    val vkUserId: String?,
    val createdAt: Instant
)

data class ServiceItem(
    val id: String,
    val code: String,
    val category: String,
    val name: String,
    val variant: String,
    val priceKopecks: Long,
    val durationMinutes: Int,
    val bufferMinutes: Int,
    val active: Boolean
)

data class WorkingWindow(
    val id: String,
    val date: LocalDate,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
)

data class CalendarEntry(
    val id: String,
    val kind: AppointmentKind,
    val clientId: String?,
    val clientNameSnapshot: String,
    val serviceId: String?,
    val serviceNameSnapshot: String,
    val source: AppointmentSource,
    val status: AppointmentStatus,
    val startsAt: Instant,
    val endsAt: Instant,
    val occupiedUntil: Instant,
    val durationMinutes: Int,
    val bufferMinutes: Int,
    val quotedPriceKopecks: Long,
    val note: String,
    val deletedAt: Instant? = null
)

data class CalendarEvent(
    val id: String,
    val entryId: String,
    val action: String,
    val actor: String,
    val summary: String,
    val occurredAt: Instant
)

data class FinanceTransaction(
    val id: String,
    val entryId: String?,
    val kind: FinanceKind,
    val category: FinanceCategory,
    val amountKopecks: Long,
    val occurredAt: Instant,
    val note: String,
    val correctionOfId: String? = null,
    val idempotencyKey: String? = null
)

data class MonthClosure(
    val id: String,
    val year: Int,
    val month: Int,
    val version: Int,
    val incomeKopecks: Long,
    val expenseKopecks: Long,
    val balanceKopecks: Long,
    val isClosed: Boolean,
    val closedAt: Instant,
    val closedBy: String,
    val reopenedAt: Instant? = null,
    val reopenReason: String? = null
) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}

data class MaterialItem(
    val id: String,
    val name: String,
    val unit: MaterialUnit,
    val stockMilliunits: Long,
    val lowStockThresholdMilliunits: Long
) {
    val isLowStock: Boolean
        get() = lowStockThresholdMilliunits > 0L && stockMilliunits <= lowStockThresholdMilliunits
}

data class MaterialMovement(
    val id: String,
    val materialId: String,
    val deltaMilliunits: Long,
    val reason: String,
    val actor: String,
    val occurredAt: Instant
)

data class LegacyVisit(
    val id: String,
    val sourceRef: String,
    val visitDate: LocalDate,
    val serviceTitle: String,
    val amountOriginKopecks: Long,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int,
    val clientId: String?,
    val clientRawName: String,
    val isUnmatched: Boolean
)

data class LegacyMonthlySummary(
    val id: String,
    val yearMonthLabel: String,
    val sourcePaidKopecks: Long,
    val sourceCostKopecks: Long,
    val uniqueClients: Int,
    val visitsCount: Int,
    val note: String
)

data class LegacyCatalogItem(
    val id: String,
    val sourceRef: String,
    val name: String,
    val category: String,
    val sourcePriceKopecks: Long
)

data class FinanceSummary(
    val yearMonth: YearMonth,
    val incomeKopecks: Long,
    val expenseKopecks: Long,
    val balanceKopecks: Long,
    val isClosed: Boolean,
    val latestClosure: MonthClosure?,
    val closures: List<MonthClosure>,
    val expenseByCategory: Map<FinanceCategory, Long>,
    val weeklyIncomeKopecks: List<Long>,
    val weeklyExpenseKopecks: List<Long>
)

data class ProductionReport(
    val yearMonth: YearMonth,
    val totalVisits: Int,
    val completedCount: Int,
    val confirmedCount: Int,
    val cancelledCount: Int,
    val noShowCount: Int,
    val pendingCount: Int,
    val expiredCount: Int,
    val workedMinutes: Int,
    val availableMinutes: Int,
    val utilizationPercent: Int?,
    val completedByService: List<Pair<String, Int>>
)

data class TodayDashboard(
    val date: LocalDate,
    val appointmentsTodayCount: Int,
    val actualCashReceivedTodayKopecks: Long,
    val utilizationPercent: Int?,
    val upcomingEntries: List<CalendarEntry>
)
