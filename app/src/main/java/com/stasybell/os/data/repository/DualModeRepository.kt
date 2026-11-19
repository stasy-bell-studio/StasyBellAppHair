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
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.security.SecureTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DualModeRepository(
    private val localRepo: LocalDemoRepository,
    private val remoteRepo: RemoteFastApiRepository,
    private val tokenStore: SecureTokenStore
) : StasyBellRepository {

    private val _mode = MutableStateFlow(tokenStore.getDataMode())
    private fun active(): StasyBellRepository =
        if (_mode.value == DataMode.DEMO_ROOM) localRepo else remoteRepo

    override val dataMode: Flow<DataMode> = _mode
    override val isAuthenticated: Flow<Boolean> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.isAuthenticated else remoteRepo.isAuthenticated
    }
    override val sessions: Flow<List<OwnerSession>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.sessions else remoteRepo.sessions
    }
    override val clients: Flow<List<Client>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.clients else remoteRepo.clients
    }
    override val services: Flow<List<ServiceItem>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.services else remoteRepo.services
    }
    override val workingWindows: Flow<List<WorkingWindow>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.workingWindows else remoteRepo.workingWindows
    }
    override val calendarEntries: Flow<List<CalendarEntry>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.calendarEntries else remoteRepo.calendarEntries
    }
    override val financeTransactions: Flow<List<FinanceTransaction>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.financeTransactions else remoteRepo.financeTransactions
    }
    override val monthClosures: Flow<List<MonthClosure>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.monthClosures else remoteRepo.monthClosures
    }
    override val materials: Flow<List<MaterialItem>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.materials else remoteRepo.materials
    }
    override val materialMovements: Flow<List<MaterialMovement>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.materialMovements else remoteRepo.materialMovements
    }
    override val legacyVisits: Flow<List<LegacyVisit>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.legacyVisits else remoteRepo.legacyVisits
    }
    override val legacyMonthlySummaries: Flow<List<LegacyMonthlySummary>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.legacyMonthlySummaries else remoteRepo.legacyMonthlySummaries
    }
    override val legacyCatalogItems: Flow<List<LegacyCatalogItem>> = _mode.flatMapLatest {
        if (it == DataMode.DEMO_ROOM) localRepo.legacyCatalogItems else remoteRepo.legacyCatalogItems
    }

    override suspend fun initialize() {
        localRepo.initialize()
        if (_mode.value == DataMode.REMOTE_FASTAPI) {
            remoteRepo.initialize()
        }
    }

    override suspend fun checkServerHealth(url: String, allowDebugHttp: Boolean): Result<String> =
        remoteRepo.checkServerHealth(url, allowDebugHttp)

    override suspend fun switchDataMode(mode: DataMode, serverUrl: String, allowDebugHttp: Boolean) {
        if (mode == DataMode.REMOTE_FASTAPI) {
            val validated = StudioRules.validateServerUrl(serverUrl, allowDebugHttp)
            tokenStore.setServerBaseUrl(validated)
            tokenStore.setAllowDebugHttp(allowDebugHttp)
            remoteRepo.rebuildClient(validated, allowDebugHttp)
        }
        tokenStore.setDataMode(mode)
        _mode.value = mode
        active().initialize()
    }

    override suspend fun login(username: String, password: String, deviceName: String) =
        active().login(username, password, deviceName)

    override suspend fun restoreSessionIfValid() = active().restoreSessionIfValid()
    override suspend fun logout() = active().logout()
    override suspend fun revokeSession(sessionId: String) = active().revokeSession(sessionId)

    override suspend fun upsertClient(id: String?, name: String, phone: String?, notes: String, preferredChannel: PreferredChannel) =
        active().upsertClient(id, name, phone, notes, preferredChannel)

    override suspend fun upsertService(
        id: String?,
        category: String,
        name: String,
        variant: String,
        priceKopecks: Long,
        durationMinutes: Int,
        bufferMinutes: Int,
        active: Boolean
    ) = active().upsertService(id, category, name, variant, priceKopecks, durationMinutes, bufferMinutes, active)

    override suspend fun saveWorkingWindows(date: LocalDate, windows: List<Pair<Int, Int>>) =
        active().saveWorkingWindows(date, windows)

    override suspend fun createManualAppointment(
        clientId: String,
        serviceId: String?,
        serviceNameOverride: String,
        source: AppointmentSource,
        startsAt: Instant,
        durationMinutes: Int,
        bufferMinutes: Int,
        quotedPriceKopecks: Long,
        note: String,
        idempotencyKey: String?
    ) = active().createManualAppointment(
        clientId, serviceId, serviceNameOverride, source, startsAt, durationMinutes, bufferMinutes, quotedPriceKopecks, note, idempotencyKey
    )

    override suspend fun createBlock(startsAt: Instant, durationMinutes: Int, note: String) =
        active().createBlock(startsAt, durationMinutes, note)

    override suspend fun moveEntry(entryId: String, newStartsAt: Instant) =
        active().moveEntry(entryId, newStartsAt)

    override suspend fun updateEntryStatus(entryId: String, newStatus: AppointmentStatus) =
        active().updateEntryStatus(entryId, newStatus)

    override suspend fun softDeleteEntry(entryId: String) = active().softDeleteEntry(entryId)
    override suspend fun restoreEntry(entryId: String) = active().restoreEntry(entryId)
    override fun observeEntryEvents(entryId: String) = active().observeEntryEvents(entryId)

    override suspend fun addFinanceTransaction(
        kind: FinanceKind,
        category: FinanceCategory,
        amountKopecks: Long,
        occurredAt: Instant,
        note: String,
        entryId: String?,
        idempotencyKey: String?
    ) = active().addFinanceTransaction(kind, category, amountKopecks, occurredAt, note, entryId, idempotencyKey)

    override suspend fun reverseFinanceTransaction(transactionId: String) =
        active().reverseFinanceTransaction(transactionId)

    override suspend fun closeMonth(yearMonth: YearMonth) = active().closeMonth(yearMonth)
    override suspend fun reopenMonth(yearMonth: YearMonth, reason: String) = active().reopenMonth(yearMonth, reason)

    override suspend fun upsertMaterial(id: String?, name: String, unit: MaterialUnit, lowStockThresholdMilliunits: Long) =
        active().upsertMaterial(id, name, unit, lowStockThresholdMilliunits)

    override suspend fun recordMaterialMovement(materialId: String, deltaMilliunits: Long, reason: String) =
        active().recordMaterialMovement(materialId, deltaMilliunits, reason)

    override suspend fun linkLegacyVisitToClient(legacyVisitId: String, clientId: String) =
        active().linkLegacyVisitToClient(legacyVisitId, clientId)
}
