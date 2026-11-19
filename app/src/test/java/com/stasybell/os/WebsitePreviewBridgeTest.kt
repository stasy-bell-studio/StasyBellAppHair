package com.stasybell.os

import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.ServiceItem
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.ui.screens.WebSyncPayloadDto
import com.stasybell.os.ui.screens.WebsitePreviewBridge
import com.stasybell.os.ui.viewmodel.AppUiState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WebsitePreviewBridgeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `sync payload serializes active services, 30-day working windows, and blocks only active non-deleted entries`() {
        val today = LocalDate.of(2026, 5, 10)
        val activeStart = StudioRules.toInstant(today, 10 * 60)
        val cancelledStart = StudioRules.toInstant(today, 15 * 60)

        val state = AppUiState(
            services = listOf(
                ServiceItem("s1", "KB-1", "Кератин / Ботокс", "Кератин", "По лопатки", 690_000, 180, 15, true),
                ServiceItem("s2", "OLD", "Архив", "Скрытая услуга", "Стандарт", 100_000, 60, 15, false)
            ),
            workingWindows = listOf(
                WorkingWindow("w1", today, 10 * 60, 14 * 60),
                WorkingWindow("w2", today, 14 * 60 + 30, 20 * 60)
            ),
            calendarEntries = listOf(
                CalendarEntry(
                    id = "e1",
                    kind = AppointmentKind.APPOINTMENT,
                    clientId = "c1",
                    clientNameSnapshot = "Анна",
                    serviceId = "s1",
                    serviceNameSnapshot = "Кератин",
                    source = AppointmentSource.WEBSITE,
                    status = AppointmentStatus.CONFIRMED,
                    startsAt = activeStart,
                    endsAt = activeStart.plusSeconds(180 * 60L),
                    occupiedUntil = activeStart.plusSeconds(195 * 60L),
                    durationMinutes = 180,
                    bufferMinutes = 15,
                    quotedPriceKopecks = 690_000,
                    note = "",
                    deletedAt = null
                ),
                CalendarEntry(
                    id = "e2",
                    kind = AppointmentKind.APPOINTMENT,
                    clientId = "c2",
                    clientNameSnapshot = "Отменённая",
                    serviceId = "s1",
                    serviceNameSnapshot = "Кератин",
                    source = AppointmentSource.PHONE,
                    status = AppointmentStatus.CANCELLED,
                    startsAt = cancelledStart,
                    endsAt = cancelledStart.plusSeconds(120 * 60L),
                    occupiedUntil = cancelledStart.plusSeconds(135 * 60L),
                    durationMinutes = 120,
                    bufferMinutes = 15,
                    quotedPriceKopecks = 690_000,
                    note = "",
                    deletedAt = null
                ),
                CalendarEntry(
                    id = "e3",
                    kind = AppointmentKind.APPOINTMENT,
                    clientId = "c3",
                    clientNameSnapshot = "Удалённая",
                    serviceId = "s1",
                    serviceNameSnapshot = "Кератин",
                    source = AppointmentSource.PHONE,
                    status = AppointmentStatus.CONFIRMED,
                    startsAt = cancelledStart,
                    endsAt = cancelledStart.plusSeconds(120 * 60L),
                    occupiedUntil = cancelledStart.plusSeconds(135 * 60L),
                    durationMinutes = 120,
                    bufferMinutes = 15,
                    quotedPriceKopecks = 690_000,
                    note = "",
                    deletedAt = Instant.now()
                )
            )
        )

        val raw = WebsitePreviewBridge.buildSyncPayloadJson(state, today)
        val parsed = json.decodeFromString<WebSyncPayloadDto>(raw)

        assertEquals(1, parsed.services.size)
        assertEquals(6900L, parsed.services.first().priceRub)
        assertEquals(30, parsed.workingWindowsByDate.size)
        assertEquals(2, parsed.workingWindowsByDate[today.toString()]?.size)

        val todayOccupied = parsed.occupiedByDate[today.toString()].orEmpty()
        assertEquals(1, todayOccupied.size)
        assertEquals(10 * 60, todayOccupied.first().startMin)
        assertEquals(13 * 60 + 15, todayOccupied.first().endMin)
    }

    @Test
    fun `incoming booking parser and MIME resolver handle valid and malformed inputs`() {
        val dto = WebsitePreviewBridge.parseIncomingBooking(
            """{"serviceId":"srv_kb_3","serviceTitle":"Кератин · По лопатки","dateIso":"2026-05-12","slotMin":870,"durationMin":180,"bufferMin":15,"priceRub":6900,"priceKopecks":690000,"clientName":"Ольга","clientContact":"@olga_spb","channel":"telegram","comment":"Тонкие волосы","idempotencyKey":"key-1"}"""
        )
        assertNotNull(dto)
        assertEquals("Ольга", dto!!.clientName)
        assertEquals(870, dto.slotMin)
        assertEquals(690_000L, dto.priceKopecks)

        assertNull(WebsitePreviewBridge.parseIncomingBooking("not-valid-json"))
        assertTrue(WebsitePreviewBridge.mimeTypeForPath("index.html").startsWith("text/html"))
        assertTrue(WebsitePreviewBridge.mimeTypeForPath("styles.css").startsWith("text/css"))
        assertTrue(WebsitePreviewBridge.mimeTypeForPath("app.js").startsWith("application/javascript"))
        assertEquals("image/jpeg", WebsitePreviewBridge.mimeTypeForPath("assets/hero-silk.jpg"))
    }
}
