package com.stasybell.os.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthDto(
    val status: String = "ok"
)

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    @SerialName("device_name") val deviceName: String
)

@Serializable
data class TokenResponseDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class SessionDto(
    val id: String,
    val username: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("last_used_at") val lastUsedAt: String,
    val revoked: Boolean
)

@Serializable
data class ClientDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val notes: String = "",
    @SerialName("preferred_channel") val preferredChannel: String = "none",
    @SerialName("telegram_chat_id") val telegramChatId: String? = null,
    @SerialName("vk_user_id") val vkUserId: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateClientDto(
    val name: String,
    val phone: String? = null,
    val notes: String = "",
    @SerialName("preferred_channel") val preferredChannel: String = "none"
)

@Serializable
data class ServiceDto(
    val id: String,
    val code: String,
    val category: String = "Услуги",
    val name: String,
    val variant: String = "",
    @SerialName("price_kopecks") val priceKopecks: Long,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("buffer_minutes") val bufferMinutes: Int,
    val active: Boolean = true
)

@Serializable
data class WorkingWindowDto(
    val id: String = "",
    val day: String,
    @SerialName("start_minute") val startMinute: Int,
    @SerialName("end_minute") val endMinute: Int
)

@Serializable
data class CalendarEntryDto(
    val id: String,
    val kind: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("client_name") val clientName: String = "",
    @SerialName("service_id") val serviceId: String? = null,
    @SerialName("service_name") val serviceName: String = "",
    val source: String,
    val status: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("occupied_until") val occupiedUntil: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("buffer_minutes") val bufferMinutes: Int,
    @SerialName("quoted_price_kopecks") val quotedPriceKopecks: Long,
    val note: String = "",
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class CreateAppointmentDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("service_id") val serviceId: String?,
    @SerialName("service_name") val serviceName: String,
    val source: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("buffer_minutes") val bufferMinutes: Int,
    @SerialName("quoted_price_kopecks") val quotedPriceKopecks: Long,
    val note: String = ""
)

@Serializable
data class CreateBlockDto(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val note: String = ""
)

@Serializable
data class MoveEntryDto(
    @SerialName("starts_at") val startsAt: String
)

@Serializable
data class StatusChangeDto(
    val status: String
)

@Serializable
data class CalendarEventDto(
    val id: String,
    @SerialName("entry_id") val entryId: String,
    val action: String,
    val actor: String,
    val summary: String,
    @SerialName("occurred_at") val occurredAt: String
)

@Serializable
data class FinanceTransactionDto(
    val id: String,
    @SerialName("entry_id") val entryId: String? = null,
    val kind: String,
    val category: String,
    @SerialName("amount_kopecks") val amountKopecks: Long,
    @SerialName("occurred_at") val occurredAt: String,
    val note: String = "",
    @SerialName("correction_of_id") val correctionOfId: String? = null
)

@Serializable
data class CreateTransactionDto(
    @SerialName("entry_id") val entryId: String? = null,
    val kind: String,
    val category: String,
    @SerialName("amount_kopecks") val amountKopecks: Long,
    @SerialName("occurred_at") val occurredAt: String,
    val note: String = ""
)

@Serializable
data class MonthClosureDto(
    val id: String,
    val year: Int,
    val month: Int,
    val version: Int,
    @SerialName("income_kopecks") val incomeKopecks: Long,
    @SerialName("expense_kopecks") val expenseKopecks: Long,
    @SerialName("balance_kopecks") val balanceKopecks: Long,
    @SerialName("is_closed") val isClosed: Boolean,
    @SerialName("closed_at") val closedAt: String,
    @SerialName("closed_by") val closedBy: String,
    @SerialName("reopened_at") val reopenedAt: String? = null,
    @SerialName("reopen_reason") val reopenReason: String? = null
)

@Serializable
data class CloseMonthDto(
    val year: Int,
    val month: Int
)

@Serializable
data class ReopenMonthDto(
    val year: Int,
    val month: Int,
    val reason: String
)

@Serializable
data class MaterialDto(
    val id: String,
    val name: String,
    val unit: String,
    @SerialName("stock_milliunits") val stockMilliunits: Long,
    @SerialName("low_stock_threshold_milliunits") val lowStockThresholdMilliunits: Long = 0
)

@Serializable
data class CreateMaterialMovementDto(
    @SerialName("delta_milliunits") val deltaMilliunits: Long,
    val reason: String
)

@Serializable
data class MaterialMovementDto(
    val id: String,
    @SerialName("material_id") val materialId: String,
    @SerialName("delta_milliunits") val deltaMilliunits: Long,
    val reason: String,
    val actor: String,
    @SerialName("occurred_at") val occurredAt: String
)
