package com.stasybell.os.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "owner_sessions")
data class OwnerSessionEntity(
    @PrimaryKey val id: String,
    val username: String,
    val deviceName: String,
    val refreshTokenHash: String,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val lastUsedAtMs: Long,
    val revoked: Boolean
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val notes: String,
    val preferredChannelCode: String,
    val telegramChatId: String?,
    val vkUserId: String?,
    val createdAtMs: Long
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val code: String,
    val category: String,
    val name: String,
    val variant: String,
    val priceKopecks: Long,
    val durationMinutes: Int,
    val bufferMinutes: Int,
    val active: Boolean
)

@Entity(
    tableName = "working_windows",
    indices = [Index(value = ["dateIso"])]
)
data class WorkingWindowEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int
)

@Entity(
    tableName = "calendar_entries",
    indices = [Index(value = ["startsAtMs"]), Index(value = ["clientId"])]
)
data class CalendarEntryEntity(
    @PrimaryKey val id: String,
    val kindCode: String,
    val clientId: String?,
    val clientNameSnapshot: String,
    val serviceId: String?,
    val serviceNameSnapshot: String,
    val sourceCode: String,
    val statusCode: String,
    val startsAtMs: Long,
    val endsAtMs: Long,
    val occupiedUntilMs: Long,
    val durationMinutes: Int,
    val bufferMinutes: Int,
    val quotedPriceKopecks: Long,
    val note: String,
    val deletedAtMs: Long?
)

@Entity(
    tableName = "calendar_events",
    indices = [Index(value = ["entryId"])]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val action: String,
    val actor: String,
    val summary: String,
    val occurredAtMs: Long
)

@Entity(
    tableName = "finance_transactions",
    indices = [Index(value = ["occurredAtMs"]), Index(value = ["idempotencyKey"], unique = true)]
)
data class FinanceTransactionEntity(
    @PrimaryKey val id: String,
    val entryId: String?,
    val kindCode: String,
    val categoryCode: String,
    val amountKopecks: Long,
    val occurredAtMs: Long,
    val note: String,
    val correctionOfId: String?,
    val idempotencyKey: String?
)

@Entity(
    tableName = "month_closures",
    indices = [Index(value = ["year", "month"])]
)
data class MonthClosureEntity(
    @PrimaryKey val id: String,
    val year: Int,
    val month: Int,
    val version: Int,
    val incomeKopecks: Long,
    val expenseKopecks: Long,
    val balanceKopecks: Long,
    val isClosed: Boolean,
    val closedAtMs: Long,
    val closedBy: String,
    val reopenedAtMs: Long?,
    val reopenReason: String?
)

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unitCode: String,
    val stockMilliunits: Long,
    val lowStockThresholdMilliunits: Long
)

@Entity(
    tableName = "material_movements",
    indices = [Index(value = ["materialId"])]
)
data class MaterialMovementEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val deltaMilliunits: Long,
    val reason: String,
    val actor: String,
    val occurredAtMs: Long
)

@Entity(
    tableName = "legacy_visits",
    indices = [Index(value = ["clientId"]), Index(value = ["visitDateIso"])]
)
data class LegacyVisitEntity(
    @PrimaryKey val id: String,
    val sourceRef: String,
    val visitDateIso: String,
    val serviceTitle: String,
    val amountOriginKopecks: Long,
    val sourceFile: String,
    val sourceSheet: String,
    val sourceRow: Int,
    val clientId: String?,
    val clientRawName: String,
    val isUnmatched: Boolean
)

@Entity(tableName = "legacy_monthly_summaries")
data class LegacyMonthlySummaryEntity(
    @PrimaryKey val id: String,
    val yearMonthLabel: String,
    val sourcePaidKopecks: Long,
    val sourceCostKopecks: Long,
    val uniqueClients: Int,
    val visitsCount: Int,
    val note: String
)

@Entity(tableName = "legacy_catalog_items")
data class LegacyCatalogItemEntity(
    @PrimaryKey val id: String,
    val sourceRef: String,
    val name: String,
    val category: String,
    val sourcePriceKopecks: Long
)
