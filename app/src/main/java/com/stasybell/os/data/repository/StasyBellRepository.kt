package com.stasybell.os.data.repository

import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.CalendarEvent
import com.stasybell.os.domain.Client
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.FinanceCategory
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.FinanceTransaction
import com.stasybell.os.domain.LegacyCatalogItem
import com.stasybell.os.domain.LegacyMonthlySummary
import com.stasybell.os.domain.LegacyVisit
import com.stasybell.os.domain.MaterialItem
import com.stasybell.os.domain.MaterialMovement
import com.stasybell.os.domain.MaterialUnit
import com.stasybell.os.domain.MonthClosure
import com.stasybell.os.domain.OwnerSession
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.domain.ServiceItem
import com.stasybell.os.domain.WorkingWindow
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

interface StasyBellRepository {
    val dataMode: Flow<DataMode>
    val isAuthenticated: Flow<Boolean>
    val sessions: Flow<List<OwnerSession>>
    val clients: Flow<List<Client>>
    val services: Flow<List<ServiceItem>>
    val workingWindows: Flow<List<WorkingWindow>>
    val calendarEntries: Flow<List<CalendarEntry>>
    val financeTransactions: Flow<List<FinanceTransaction>>
    val monthClosures: Flow<List<MonthClosure>>
    val materials: Flow<List<MaterialItem>>
    val materialMovements: Flow<List<MaterialMovement>>
    val legacyVisits: Flow<List<LegacyVisit>>
    val legacyMonthlySummaries: Flow<List<LegacyMonthlySummary>>
    val legacyCatalogItems: Flow<List<LegacyCatalogItem>>

    suspend fun initialize()
    suspend fun checkServerHealth(url: String, allowDebugHttp: Boolean): Result<String>
    suspend fun switchDataMode(mode: DataMode, serverUrl: String, allowDebugHttp: Boolean)

    // Auth & Sessions
    suspend fun login(username: String, password: String, deviceName: String): OwnerSession
    suspend fun restoreSessionIfValid(): Boolean
    suspend fun logout()
    suspend fun revokeSession(sessionId: String)

    // Clients
    suspend fun upsertClient(
        id: String?,
        name: String,
        phone: String?,
        notes: String,
        preferredChannel: PreferredChannel
    ): Client

    // Services
    suspend fun upsertService(
        id: String?,
        category: String,
        name: String,
        variant: String,
        priceKopecks: Long,
        durationMinutes: Int,
        bufferMinutes: Int,
        active: Boolean
    ): ServiceItem

    // Schedule
    suspend fun saveWorkingWindows(date: LocalDate, windows: List<Pair<Int, Int>>)

    // Calendar
    suspend fun createManualAppointment(
        clientId: String,
        serviceId: String?,
        serviceNameOverride: String,
        source: AppointmentSource,
        startsAt: Instant,
        durationMinutes: Int,
        bufferMinutes: Int,
        quotedPriceKopecks: Long,
        note: String,
        idempotencyKey: String? = null
    ): CalendarEntry

    suspend fun createBlock(
        startsAt: Instant,
        durationMinutes: Int,
        note: String
    ): CalendarEntry

    suspend fun moveEntry(entryId: String, newStartsAt: Instant): CalendarEntry
    suspend fun updateEntryStatus(entryId: String, newStatus: AppointmentStatus): CalendarEntry
    suspend fun softDeleteEntry(entryId: String): CalendarEntry
    suspend fun restoreEntry(entryId: String): CalendarEntry
    fun observeEntryEvents(entryId: String): Flow<List<CalendarEvent>>

    // Finances (Manual Actual Cash Only)
    suspend fun addFinanceTransaction(
        kind: FinanceKind,
        category: FinanceCategory,
        amountKopecks: Long,
        occurredAt: Instant,
        note: String,
        entryId: String? = null,
        idempotencyKey: String? = null
    ): FinanceTransaction

    suspend fun reverseFinanceTransaction(transactionId: String): FinanceTransaction
    suspend fun closeMonth(yearMonth: YearMonth): MonthClosure
    suspend fun reopenMonth(yearMonth: YearMonth, reason: String): MonthClosure

    // Materials
    suspend fun upsertMaterial(
        id: String?,
        name: String,
        unit: MaterialUnit,
        lowStockThresholdMilliunits: Long
    ): MaterialItem

    suspend fun recordMaterialMovement(
        materialId: String,
        deltaMilliunits: Long,
        reason: String
    ): MaterialMovement

    // Legacy Archive Reconciliation
    suspend fun linkLegacyVisitToClient(legacyVisitId: String, clientId: String)
}
