package com.stasybell.os.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stasybell.os.data.repository.StasyBellRepository
import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.CalendarEvent
import com.stasybell.os.domain.Client
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.DomainException
import com.stasybell.os.domain.FinanceCategory
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.FinanceSummary
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
import com.stasybell.os.domain.ProductionReport
import com.stasybell.os.domain.ServiceItem
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.TodayDashboard
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.security.SecureTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class AppUiState(
    val isInitialized: Boolean = false,
    val isAuthenticated: Boolean = false,
    val dataMode: DataMode = DataMode.DEMO_ROOM,
    val biometricEnabled: Boolean = false,
    val serverUrl: String = "",
    val allowDebugHttp: Boolean = false,
    val serverHealthMessage: String? = null,
    val isBusy: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(StudioRules.STUDIO_ZONE),
    val selectedMonth: YearMonth = YearMonth.now(StudioRules.STUDIO_ZONE),
    val sessions: List<OwnerSession> = emptyList(),
    val clients: List<Client> = emptyList(),
    val services: List<ServiceItem> = emptyList(),
    val workingWindows: List<WorkingWindow> = emptyList(),
    val calendarEntries: List<CalendarEntry> = emptyList(),
    val financeTransactions: List<FinanceTransaction> = emptyList(),
    val monthClosures: List<MonthClosure> = emptyList(),
    val materials: List<MaterialItem> = emptyList(),
    val materialMovements: List<MaterialMovement> = emptyList(),
    val legacyVisits: List<LegacyVisit> = emptyList(),
    val legacyMonthlySummaries: List<LegacyMonthlySummary> = emptyList(),
    val legacyCatalogItems: List<LegacyCatalogItem> = emptyList()
) {
    val todayDashboard: TodayDashboard
        get() {
            val today = LocalDate.now(StudioRules.STUDIO_ZONE)
            val todayEntries = calendarEntries
                .filter { it.deletedAt == null && StudioRules.toLocalDate(it.startsAt) == today }
                .sortedBy { it.startsAt }
            val activeTodayAppointments = todayEntries.filter {
                it.kind == AppointmentKind.APPOINTMENT &&
                    it.status != AppointmentStatus.CANCELLED &&
                    it.status != AppointmentStatus.EXPIRED
            }
            val todayWindows = workingWindows.filter { it.date == today }
            val utilization = StudioRules.computeUtilizationPercent(todayWindows, todayEntries)
            // CRITICAL INVARIANT: Actual cash received today comes ONLY from manual finance transactions
            val actualReceivedToday = financeTransactions
                .filter { it.kind == FinanceKind.INCOME && StudioRules.toLocalDate(it.occurredAt) == today }
                .sumOf { it.amountKopecks }

            return TodayDashboard(
                date = today,
                appointmentsTodayCount = activeTodayAppointments.size,
                actualCashReceivedTodayKopecks = actualReceivedToday,
                utilizationPercent = utilization,
                upcomingEntries = todayEntries.filter {
                    it.status != AppointmentStatus.CANCELLED && it.status != AppointmentStatus.EXPIRED
                }
            )
        }

    fun financeSummaryFor(month: YearMonth): FinanceSummary =
        StudioRules.computeFinanceSummary(month, financeTransactions, monthClosures)

    fun productionReportFor(month: YearMonth): ProductionReport {
        val monthEntries = calendarEntries.filter {
            it.deletedAt == null &&
                it.kind == AppointmentKind.APPOINTMENT &&
                StudioRules.toYearMonth(it.startsAt) == month
        }
        val completed = monthEntries.filter { it.status == AppointmentStatus.COMPLETED }
        val monthWindows = workingWindows.filter { YearMonth.from(it.date) == month }
        val availableMinutes = monthWindows.sumOf { (it.endMinuteOfDay - it.startMinuteOfDay).coerceAtLeast(0) }
        val workedMinutes = completed.sumOf { it.durationMinutes }
        val utilization = if (availableMinutes > 0) {
            ((workedMinutes * 100.0) / availableMinutes).toInt().coerceIn(0, 100)
        } else {
            null
        }
        val byService = completed
            .groupBy { it.serviceNameSnapshot }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }

        return ProductionReport(
            yearMonth = month,
            totalVisits = monthEntries.size,
            completedCount = completed.size,
            confirmedCount = monthEntries.count { it.status == AppointmentStatus.CONFIRMED },
            cancelledCount = monthEntries.count { it.status == AppointmentStatus.CANCELLED },
            noShowCount = monthEntries.count { it.status == AppointmentStatus.NO_SHOW },
            pendingCount = monthEntries.count { it.status == AppointmentStatus.PENDING_CHANNEL },
            expiredCount = monthEntries.count { it.status == AppointmentStatus.EXPIRED },
            workedMinutes = workedMinutes,
            availableMinutes = availableMinutes,
            utilizationPercent = utilization,
            completedByService = byService
        )
    }
}

sealed class UiMessage {
    data class Success(val text: String) : UiMessage()
    data class Error(val text: String) : UiMessage()
}

class MainViewModel(
    private val repository: StasyBellRepository,
    private val tokenStore: SecureTokenStore,
    initiallyInitialized: Boolean = false
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now(StudioRules.STUDIO_ZONE))
    private val _selectedMonth = MutableStateFlow(YearMonth.now(StudioRules.STUDIO_ZONE))
    private val _isBusy = MutableStateFlow(false)
    private val _isInitialized = MutableStateFlow(initiallyInitialized)
    private val _biometricEnabled = MutableStateFlow(tokenStore.isBiometricEnabled())
    private val _serverUrl = MutableStateFlow(tokenStore.getServerBaseUrl())
    private val _allowDebugHttp = MutableStateFlow(tokenStore.getAllowDebugHttp())
    private val _serverHealth = MutableStateFlow<String?>(null)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 10)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private val coreDataFlow = combine(
        repository.isAuthenticated,
        repository.dataMode,
        repository.sessions,
        repository.clients,
        repository.services
    ) { auth, mode, sessions, clients, services ->
        CoreBundle(auth, mode, sessions, clients, services)
    }

    private val opsDataFlow = combine(
        repository.workingWindows,
        repository.calendarEntries,
        repository.financeTransactions,
        repository.monthClosures,
        repository.materials
    ) { windows, entries, txs, closures, materials ->
        OpsBundle(windows, entries, txs, closures, materials)
    }

    private val archiveAndMetaFlow = combine(
        repository.materialMovements,
        repository.legacyVisits,
        repository.legacyMonthlySummaries,
        repository.legacyCatalogItems,
        _selectedDate
    ) { movements, legacyVisits, legacySummaries, legacyCatalog, selectedDate ->
        ArchiveBundle(movements, legacyVisits, legacySummaries, legacyCatalog, selectedDate)
    }

    private val settingsFlow = combine(
        _isInitialized,
        _isBusy,
        _selectedMonth,
        _biometricEnabled,
        combine(_serverUrl, _allowDebugHttp, _serverHealth) { u, h, s -> Triple(u, h, s) }
    ) { init, busy, month, bio, srv ->
        SettingsBundle(init, busy, month, bio, srv.first, srv.second, srv.third)
    }

    val uiState: StateFlow<AppUiState> = combine(
        coreDataFlow,
        opsDataFlow,
        archiveAndMetaFlow,
        settingsFlow
    ) { core, ops, arch, set ->
        AppUiState(
            isInitialized = set.initialized,
            isAuthenticated = core.isAuthenticated,
            dataMode = core.dataMode,
            biometricEnabled = set.biometricEnabled,
            serverUrl = set.serverUrl,
            allowDebugHttp = set.allowDebugHttp,
            serverHealthMessage = set.serverHealth,
            isBusy = set.busy,
            selectedDate = arch.selectedDate,
            selectedMonth = set.selectedMonth,
            sessions = core.sessions,
            clients = core.clients,
            services = core.services,
            workingWindows = ops.windows,
            calendarEntries = ops.entries,
            financeTransactions = ops.transactions,
            monthClosures = ops.closures,
            materials = ops.materials,
            materialMovements = arch.movements,
            legacyVisits = arch.legacyVisits,
            legacyMonthlySummaries = arch.legacySummaries,
            legacyCatalogItems = arch.legacyCatalog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState(isInitialized = initiallyInitialized)
    )

    init {
        viewModelScope.launch {
            runCatching { repository.initialize() }
            _isInitialized.value = true
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun observeEntryEvents(entryId: String): Flow<List<CalendarEvent>> =
        repository.observeEntryEvents(entryId)

    private fun runAction(successMessage: String? = null, onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                block()
                if (successMessage != null) {
                    _messages.tryEmit(UiMessage.Success(successMessage))
                }
                onSuccess()
            } catch (e: DomainException) {
                _messages.tryEmit(UiMessage.Error(e.message))
            } catch (e: Exception) {
                _messages.tryEmit(UiMessage.Error(e.message ?: "Произошла ошибка операции"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun login(username: String, password: String) {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifEmpty { "Android" }
        runAction(successMessage = "Добро пожаловать, Настя") {
            repository.login(username, password, deviceName)
        }
    }

    fun loginWithBiometricSuccess() {
        runAction(successMessage = "Сессия восстановлена по биометрии") {
            val ok = repository.restoreSessionIfValid()
            if (!ok) {
                throw DomainException(
                    com.stasybell.os.domain.ErrorCode.AUTH_FAILED,
                    "Активная сессия не найдена — войдите по паролю"
                )
            }
        }
    }

    fun logout() {
        runAction(successMessage = "Вы вышли из аккаунта") {
            repository.logout()
        }
    }

    fun revokeSession(sessionId: String) {
        runAction(successMessage = "Сессия отозвана") {
            repository.revokeSession(sessionId)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        tokenStore.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
        _messages.tryEmit(
            UiMessage.Success(if (enabled) "Вход по биометрии включён" else "Вход по биометрии отключён")
        )
    }

    fun checkServerHealth(url: String, allowDebugHttp: Boolean) {
        runAction {
            val result = repository.checkServerHealth(url, allowDebugHttp)
            result.onSuccess { msg ->
                _serverHealth.value = msg
                _messages.tryEmit(UiMessage.Success(msg))
            }.onFailure { err ->
                val text = err.message ?: "Сервер недоступен"
                _serverHealth.value = text
                _messages.tryEmit(UiMessage.Error(text))
            }
        }
    }

    fun switchDataMode(mode: DataMode, serverUrl: String, allowDebugHttp: Boolean) {
        runAction(successMessage = "Режим переключён: ${mode.labelRu}") {
            repository.switchDataMode(mode, serverUrl, allowDebugHttp)
            _serverUrl.value = tokenStore.getServerBaseUrl()
            _allowDebugHttp.value = tokenStore.getAllowDebugHttp()
        }
    }

    fun saveClient(
        id: String?,
        name: String,
        phone: String?,
        notes: String,
        preferredChannel: PreferredChannel,
        onSaved: (Client) -> Unit = {}
    ) {
        runAction(successMessage = if (id == null) "Клиент добавлен" else "Карточка клиента обновлена") {
            val saved = repository.upsertClient(id, name, phone, notes, preferredChannel)
            onSaved(saved)
        }
    }

    fun saveService(
        id: String?,
        category: String,
        name: String,
        variant: String,
        priceKopecks: Long,
        durationMinutes: Int,
        bufferMinutes: Int,
        active: Boolean,
        onSaved: () -> Unit = {}
    ) {
        runAction(
            successMessage = if (id == null) "Услуга добавлена" else "Услуга обновлена (история записей сохранена)",
            onSuccess = onSaved
        ) {
            repository.upsertService(id, category, name, variant, priceKopecks, durationMinutes, bufferMinutes, active)
        }
    }

    fun saveWorkingWindows(date: LocalDate, windows: List<Pair<Int, Int>>, onSaved: () -> Unit = {}) {
        runAction(successMessage = "График на ${StudioRules.formatDate(date)} сохранён", onSuccess = onSaved) {
            repository.saveWorkingWindows(date, windows)
        }
    }

    fun createManualAppointment(
        clientId: String,
        serviceId: String?,
        serviceName: String,
        source: AppointmentSource,
        startsAt: Instant,
        durationMinutes: Int,
        bufferMinutes: Int,
        quotedPriceKopecks: Long,
        note: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
        onCreated: (CalendarEntry) -> Unit = {}
    ) {
        runAction(successMessage = "Запись подтверждена и добавлена в календарь") {
            val created = repository.createManualAppointment(
                clientId = clientId,
                serviceId = serviceId,
                serviceNameOverride = serviceName,
                source = source,
                startsAt = startsAt,
                durationMinutes = durationMinutes,
                bufferMinutes = bufferMinutes,
                quotedPriceKopecks = quotedPriceKopecks,
                note = note,
                idempotencyKey = idempotencyKey
            )
            onCreated(created)
        }
    }

    fun createBlock(startsAt: Instant, durationMinutes: Int, note: String, onCreated: () -> Unit = {}) {
        runAction(successMessage = "Блокировка добавлена", onSuccess = onCreated) {
            repository.createBlock(startsAt, durationMinutes, note)
        }
    }

    fun moveEntry(entryId: String, newStartsAt: Instant, onMoved: () -> Unit = {}) {
        runAction(successMessage = "Запись перенесена", onSuccess = onMoved) {
            repository.moveEntry(entryId, newStartsAt)
        }
    }

    fun updateEntryStatus(entryId: String, newStatus: AppointmentStatus) {
        val hint = if (newStatus == AppointmentStatus.COMPLETED) {
            "Визит завершён. Фактическую оплату внесите вручную в кассу."
        } else {
            "Статус: ${newStatus.labelRu}"
        }
        runAction(successMessage = hint) {
            repository.updateEntryStatus(entryId, newStatus)
        }
    }

    fun softDeleteEntry(entryId: String, onDeleted: () -> Unit = {}) {
        runAction(successMessage = "Запись удалена (доступно восстановление)", onSuccess = onDeleted) {
            repository.softDeleteEntry(entryId)
        }
    }

    fun restoreEntry(entryId: String) {
        runAction(successMessage = "Запись восстановлена") {
            repository.restoreEntry(entryId)
        }
    }

    fun addFinanceTransaction(
        kind: FinanceKind,
        category: FinanceCategory,
        amountKopecks: Long,
        occurredAt: Instant,
        note: String,
        entryId: String? = null,
        idempotencyKey: String,
        onSaved: () -> Unit = {}
    ) {
        runAction(
            successMessage = "${kind.labelRu} ${StudioRules.formatRubles(amountKopecks)} записан в кассу",
            onSuccess = onSaved
        ) {
            repository.addFinanceTransaction(kind, category, amountKopecks, occurredAt, note, entryId, idempotencyKey)
        }
    }

    fun reverseFinanceTransaction(transactionId: String) {
        runAction(successMessage = "Сторно проведено (оригинал сохранён в истории)") {
            repository.reverseFinanceTransaction(transactionId)
        }
    }

    fun closeMonth(yearMonth: YearMonth) {
        runAction(successMessage = "Месяц ${StudioRules.formatMonthYear(yearMonth)} закрыт") {
            repository.closeMonth(yearMonth)
        }
    }

    fun reopenMonth(yearMonth: YearMonth, reason: String, onDone: () -> Unit = {}) {
        runAction(successMessage = "Месяц переоткрыт для правки", onSuccess = onDone) {
            repository.reopenMonth(yearMonth, reason)
        }
    }

    fun saveMaterial(
        id: String?,
        name: String,
        unit: MaterialUnit,
        lowStockThresholdMilliunits: Long,
        onSaved: () -> Unit = {}
    ) {
        runAction(successMessage = if (id == null) "Материал добавлен" else "Материал обновлён", onSuccess = onSaved) {
            repository.upsertMaterial(id, name, unit, lowStockThresholdMilliunits)
        }
    }

    fun recordMaterialMovement(
        materialId: String,
        deltaMilliunits: Long,
        reason: String,
        onSaved: () -> Unit = {}
    ) {
        val label = if (deltaMilliunits > 0) "Приход на склад записан" else "Списание со склада записано"
        runAction(successMessage = label, onSuccess = onSaved) {
            repository.recordMaterialMovement(materialId, deltaMilliunits, reason)
        }
    }

    fun linkLegacyVisitToClient(legacyVisitId: String, clientId: String) {
        runAction(successMessage = "Архивный визит привязан к клиенту") {
            repository.linkLegacyVisitToClient(legacyVisitId, clientId)
        }
    }

    fun createWebsiteTestBooking(dto: com.stasybell.os.ui.screens.WebIncomingBookingDto) {
        runAction(successMessage = "Заявка с сайта «${dto.clientName}» добавлена в календарь") {
            val channel = when (dto.channel.lowercase()) {
                "telegram" -> PreferredChannel.TELEGRAM
                "vk" -> PreferredChannel.VK
                "phone" -> PreferredChannel.PHONE
                else -> PreferredChannel.NONE
            }
            val contact = dto.clientContact.trim()
            val phone = contact.takeIf { it.any(Char::isDigit) && !it.startsWith("@") }
            val existingClient = uiState.value.clients.firstOrNull {
                it.name.equals(dto.clientName.trim(), ignoreCase = true)
            }
            val client = existingClient ?: repository.upsertClient(
                id = null,
                name = dto.clientName.trim(),
                phone = phone,
                notes = buildString {
                    append("Контакт с сайта: $contact")
                    if (dto.comment.isNotBlank()) append(" · ${dto.comment.trim()}")
                },
                preferredChannel = channel
            )
            val date = LocalDate.parse(dto.dateIso)
            val startsAt = StudioRules.toInstant(date, dto.slotMin)
            repository.createManualAppointment(
                clientId = client.id,
                serviceId = dto.serviceId.takeIf { id -> uiState.value.services.any { it.id == id } },
                serviceNameOverride = dto.serviceTitle.ifBlank { "Услуга с сайта" },
                source = AppointmentSource.WEBSITE,
                startsAt = startsAt,
                durationMinutes = dto.durationMin.coerceAtLeast(30),
                bufferMinutes = dto.bufferMin.coerceAtLeast(0),
                quotedPriceKopecks = if (dto.priceKopecks > 0L) dto.priceKopecks else dto.priceRub * 100L,
                note = dto.comment.ifBlank { "Заявка через превью сайта (${channel.labelRu})" },
                idempotencyKey = dto.idempotencyKey.ifBlank { null }
            )
        }
    }

    private data class CoreBundle(
        val isAuthenticated: Boolean,
        val dataMode: DataMode,
        val sessions: List<OwnerSession>,
        val clients: List<Client>,
        val services: List<ServiceItem>
    )

    private data class OpsBundle(
        val windows: List<WorkingWindow>,
        val entries: List<CalendarEntry>,
        val transactions: List<FinanceTransaction>,
        val closures: List<MonthClosure>,
        val materials: List<MaterialItem>
    )

    private data class ArchiveBundle(
        val movements: List<MaterialMovement>,
        val legacyVisits: List<LegacyVisit>,
        val legacySummaries: List<LegacyMonthlySummary>,
        val legacyCatalog: List<LegacyCatalogItem>,
        val selectedDate: LocalDate
    )

    private data class SettingsBundle(
        val initialized: Boolean,
        val busy: Boolean,
        val selectedMonth: YearMonth,
        val biometricEnabled: Boolean,
        val serverUrl: String,
        val allowDebugHttp: Boolean,
        val serverHealth: String?
    )

    class Factory(
        private val repository: StasyBellRepository,
        private val tokenStore: SecureTokenStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, tokenStore) as T
        }
    }
}
