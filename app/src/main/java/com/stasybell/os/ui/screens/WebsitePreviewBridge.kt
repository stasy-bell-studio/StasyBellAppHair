package com.stasybell.os.ui.screens

import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.viewmodel.AppUiState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class WebSyncServiceDto(
    val id: String,
    val category: String,
    val name: String,
    val lengthLabel: String,
    val priceRub: Long,
    val durationMin: Int,
    val bufferMin: Int,
    val active: Boolean
)

@Serializable
data class WebSyncIntervalDto(
    val startMin: Int,
    val endMin: Int
)

@Serializable
data class WebSyncPayloadDto(
    val services: List<WebSyncServiceDto>,
    val workingWindowsByDate: Map<String, List<WebSyncIntervalDto>>,
    val occupiedByDate: Map<String, List<WebSyncIntervalDto>>
)

@Serializable
data class WebIncomingBookingDto(
    val serviceId: String = "",
    val serviceTitle: String = "",
    val dateIso: String = "",
    val slotMin: Int = 600,
    val durationMin: Int = 120,
    val bufferMin: Int = 15,
    val priceRub: Long = 0L,
    val priceKopecks: Long = 0L,
    val clientName: String = "",
    val clientContact: String = "",
    val channel: String = "telegram",
    val comment: String = "",
    val idempotencyKey: String = ""
)

object WebsitePreviewBridge {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes active services, 30-day working windows, and active occupied intervals
     * from STASY BELL OS 2.0 into the exact JSON format consumed by website/app.js.
     */
    fun buildSyncPayloadJson(
        state: AppUiState,
        today: LocalDate = LocalDate.now(StudioRules.STUDIO_ZONE)
    ): String {
        val services = state.services
            .filter { it.active }
            .map { s ->
                WebSyncServiceDto(
                    id = s.id,
                    category = s.category,
                    name = s.name,
                    lengthLabel = s.variant,
                    priceRub = s.priceKopecks / 100L,
                    durationMin = s.durationMinutes,
                    bufferMin = s.bufferMinutes,
                    active = s.active
                )
            }

        val windowsByDate = linkedMapOf<String, List<WebSyncIntervalDto>>()
        val occupiedByDate = linkedMapOf<String, List<WebSyncIntervalDto>>()

        val activeBlockingStatuses = setOf(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.PENDING_CHANNEL,
            AppointmentStatus.BLOCKED,
            AppointmentStatus.COMPLETED
        )

        for (offset in 0 until StudioRules.PUBLIC_BOOKING_HORIZON_DAYS) {
            val d = today.plusDays(offset.toLong())
            val iso = d.toString()
            windowsByDate[iso] = state.workingWindows
                .filter { it.date == d }
                .sortedBy { it.startMinuteOfDay }
                .map { WebSyncIntervalDto(it.startMinuteOfDay, it.endMinuteOfDay) }

            occupiedByDate[iso] = state.calendarEntries
                .filter { entry ->
                    entry.deletedAt == null &&
                        entry.status in activeBlockingStatuses &&
                        StudioRules.toLocalDate(entry.startsAt) == d
                }
                .map { entry ->
                    val startZ = entry.startsAt.atZone(StudioRules.STUDIO_ZONE)
                    val endZ = entry.occupiedUntil.atZone(StudioRules.STUDIO_ZONE)
                    val startMin = startZ.hour * 60 + startZ.minute
                    val endMin = endZ.hour * 60 + endZ.minute
                    WebSyncIntervalDto(startMin, endMin.coerceAtLeast(startMin + 15))
                }
        }

        val payload = WebSyncPayloadDto(
            services = services,
            workingWindowsByDate = windowsByDate,
            occupiedByDate = occupiedByDate
        )
        return json.encodeToString(payload)
    }

    fun parseIncomingBooking(rawJson: String): WebIncomingBookingDto? {
        return runCatching { json.decodeFromString<WebIncomingBookingDto>(rawJson) }.getOrNull()
    }

    fun mimeTypeForPath(path: String): String {
        val clean = path.lowercase().substringBefore('?').substringBefore('#')
        return when {
            clean.endsWith(".html") -> "text/html; charset=utf-8"
            clean.endsWith(".css") -> "text/css; charset=utf-8"
            clean.endsWith(".js") -> "application/javascript; charset=utf-8"
            clean.endsWith(".json") -> "application/json; charset=utf-8"
            clean.endsWith(".jpg") || clean.endsWith(".jpeg") -> "image/jpeg"
            clean.endsWith(".png") -> "image/png"
            clean.endsWith(".svg") -> "image/svg+xml"
            clean.endsWith(".webp") -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
