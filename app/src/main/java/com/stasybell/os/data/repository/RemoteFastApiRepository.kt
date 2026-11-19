package com.stasybell.os.data.repository

import com.stasybell.os.data.remote.CalendarEntryDto
import com.stasybell.os.data.remote.CloseMonthDto
import com.stasybell.os.data.remote.CreateAppointmentDto
import com.stasybell.os.data.remote.CreateBlockDto
import com.stasybell.os.data.remote.CreateClientDto
import com.stasybell.os.data.remote.CreateMaterialMovementDto
import com.stasybell.os.data.remote.CreateTransactionDto
import com.stasybell.os.data.remote.FinanceTransactionDto
import com.stasybell.os.data.remote.LoginRequestDto
import com.stasybell.os.data.remote.MaterialDto
import com.stasybell.os.data.remote.MaterialMovementDto
import com.stasybell.os.data.remote.MonthClosureDto
import com.stasybell.os.data.remote.MoveEntryDto
import com.stasybell.os.data.remote.ReopenMonthDto
import com.stasybell.os.data.remote.RetrofitClientFactory
import com.stasybell.os.data.remote.ServiceDto
import com.stasybell.os.data.remote.StasyBellApiService
import com.stasybell.os.data.remote.StatusChangeDto
import com.stasybell.os.data.remote.WorkingWindowDto
import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.CalendarEvent
import com.stasybell.os.domain.Client
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.DomainException
import com.stasybell.os.domain.ErrorCode
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
import com.stasybell.os.security.SecureTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Real FastAPI/PostgreSQL REST repository.
 * Never fakes success when offline or server is unreachable.
 */
class RemoteFastApiRepository(
    private val tokenStore: SecureTokenStore
) : StasyBellRepository {

    private var api: StasyBellApiService? = null

    private val _isAuthenticated = MutableStateFlow(false)
    private val _sessions = MutableStateFlow<List<OwnerSession>>(emptyList())
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    private val _workingWindows = MutableStateFlow<List<WorkingWindow>>(emptyList())
    private val _calendarEntries = MutableStateFlow<List<CalendarEntry>>(emptyList())
    private val _transactions = MutableStateFlow<List<FinanceTransaction>>(emptyList())
    private val _closures = MutableStateFlow<List<MonthClosure>>(emptyList())
    private val _materials = MutableStateFlow<List<MaterialItem>>(emptyList())
    private val _movements = MutableStateFlow<List<MaterialMovement>>(emptyList())
    private val _legacyVisits = MutableStateFlow<List<LegacyVisit>>(emptyList())
    private val _legacySummaries = MutableStateFlow<List<LegacyMonthlySummary>>(emptyList())
    private val _legacyCatalog = MutableStateFlow<List<LegacyCatalogItem>>(emptyList())

    override val dataMode: Flow<DataMode> = MutableStateFlow(DataMode.REMOTE_FASTAPI)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated
    override val sessions: Flow<List<OwnerSession>> = _sessions
    override val clients: Flow<List<Client>> = _clients
    override val services: Flow<List<ServiceItem>> = _services
    override val workingWindows: Flow<List<WorkingWindow>> = _workingWindows
    override val calendarEntries: Flow<List<CalendarEntry>> = _calendarEntries
    override val financeTransactions: Flow<List<FinanceTransaction>> = _transactions
    override val monthClosures: Flow<List<MonthClosure>> = _closures
    override val materials: Flow<List<MaterialItem>> = _materials
    override val materialMovements: Flow<List<MaterialMovement>> = _movements
    override val legacyVisits: Flow<List<LegacyVisit>> = _legacyVisits
    override val legacyMonthlySummaries: Flow<List<LegacyMonthlySummary>> = _legacySummaries
    override val legacyCatalogItems: Flow<List<LegacyCatalogItem>> = _legacyCatalog

    private fun requireApi(): StasyBellApiService {
        val current = api
        if (current != null) return current
        val url = tokenStore.getServerBaseUrl()
        val created = RetrofitClientFactory.create(url, tokenStore.getAllowDebugHttp(), tokenStore)
        api = created
        return created
    }

    fun rebuildClient(url: String, allowDebugHttp: Boolean) {
        api = RetrofitClientFactory.create(url, allowDebugHttp, tokenStore)
    }

    private suspend fun <T> executeCall(block: suspend (StasyBellApiService) -> Response<T>): T {
        val service = requireApi()
        val response = try {
            block(service)
        } catch (e: IOException) {
            throw DomainException(ErrorCode.NETWORK_ERROR, "Не удалось сохранить: нет связи с сервером")
        }
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            return response.body() as T
        }
        when (response.code()) {
            401, 403 -> throw DomainException(ErrorCode.AUTH_FAILED, "Сессия истекла или доступ запрещён")
            409 -> throw DomainException(ErrorCode.SLOT_CONFLICT, "Время уже занято или конфликт данных (409)")
            422 -> throw DomainException(ErrorCode.INVALID_INPUT, "Проверьте корректность введённых данных")
            429 -> throw DomainException(ErrorCode.RATE_LIMITED, "Слишком много запросов. Повторите позже.")
            else -> throw DomainException(ErrorCode.NETWORK_ERROR, "Ошибка сервера (${response.code()})")
        }
    }

    override suspend fun initialize() {
        restoreSessionIfValid()
    }

    override suspend fun checkServerHealth(url: String, allowDebugHttp: Boolean): Result<String> {
        return runCatching {
            val probe = RetrofitClientFactory.create(url, allowDebugHttp, tokenStore)
            val resp = probe.health()
            if (resp.isSuccessful) {
                "Сервер доступен (${resp.body()?.status ?: "ok"})"
            } else {
                throw DomainException(ErrorCode.NETWORK_ERROR, "Сервер ответил кодом ${resp.code()}")
            }
        }
    }

    override suspend fun switchDataMode(mode: DataMode, serverUrl: String, allowDebugHttp: Boolean) {
        if (mode == DataMode.REMOTE_FASTAPI) {
            rebuildClient(serverUrl, allowDebugHttp)
        }
    }

    override suspend fun login(username: String, password: String, deviceName: String): OwnerSession {
        val tokens = executeCall {
            it.login(LoginRequestDto(username.trim(), password, deviceName))
        }
        val exp = Instant.parse(tokens.expiresAt)
        tokenStore.saveSessionTokens(tokens.sessionId, tokens.accessToken, tokens.refreshToken, exp.toEpochMilli())
        _isAuthenticated.value = true
        refreshAllData()
        return OwnerSession(
            id = tokens.sessionId,
            username = username.trim(),
            deviceName = deviceName,
            createdAt = Instant.now(),
            expiresAt = exp,
            lastUsedAt = Instant.now(),
            revoked = false,
            isCurrent = true
        )
    }

    override suspend fun restoreSessionIfValid(): Boolean {
        val ok = tokenStore.hasValidSession() && tokenStore.getServerBaseUrl().isNotBlank()
        _isAuthenticated.value = ok
        if (ok) {
            runCatching { refreshAllData() }
        }
        return ok
    }

    override suspend fun logout() {
        runCatching { executeCall { it.logout() } }
        tokenStore.clearSession()
        _isAuthenticated.value = false
    }

    override suspend fun revokeSession(sessionId: String) {
        executeCall { it.revokeSession(sessionId) }
        if (sessionId == tokenStore.getSessionId()) {
            tokenStore.clearSession()
            _isAuthenticated.value = false
        } else {
            refreshSessions()
        }
    }

    private suspend fun refreshAllData() {
        refreshSessions()
        _clients.value = executeCall { it.getClients() }.map { dto ->
            Client(
                id = dto.id,
                name = dto.name,
                phone = dto.phone,
                notes = dto.notes,
                preferredChannel = PreferredChannel.fromCode(dto.preferredChannel),
                telegramChatId = dto.telegramChatId,
                vkUserId = dto.vkUserId,
                createdAt = Instant.parse(dto.createdAt)
            )
        }
        _services.value = executeCall { it.getServices() }.map { it.toDomain() }
        _calendarEntries.value = executeCall { it.getCalendar() }.map { it.toDomain() }
        _transactions.value = executeCall { it.getTransactions() }.map { it.toDomain() }
        _closures.value = executeCall { it.getClosures() }.map { it.toDomain() }
        _materials.value = executeCall { it.getMaterials() }.map { it.toDomain() }
    }

    private suspend fun refreshSessions() {
        val currentSid = tokenStore.getSessionId()
        _sessions.value = executeCall { it.getSessions() }.map { dto ->
            OwnerSession(
                id = dto.id,
                username = dto.username,
                deviceName = dto.deviceName,
                createdAt = Instant.parse(dto.createdAt),
                expiresAt = Instant.parse(dto.expiresAt),
                lastUsedAt = Instant.parse(dto.lastUsedAt),
                revoked = dto.revoked,
                isCurrent = dto.id == currentSid
            )
        }
    }

    override suspend fun upsertClient(
        id: String?,
        name: String,
        phone: String?,
        notes: String,
        preferredChannel: PreferredChannel
    ): Client {
        val body = CreateClientDto(name.trim(), phone?.trim()?.takeIf { it.isNotEmpty() }, notes.trim(), preferredChannel.code)
        val dto = if (id == null) executeCall { it.createClient(body) } else executeCall { it.updateClient(id, body) }
        val domain = Client(dto.id, dto.name, dto.phone, dto.notes, PreferredChannel.fromCode(dto.preferredChannel), dto.telegramChatId, dto.vkUserId, Instant.parse(dto.createdAt))
        _clients.value = (_clients.value.filterNot { it.id == domain.id } + domain).sortedBy { it.name.lowercase() }
        return domain
    }

    override suspend fun upsertService(
        id: String?,
        category: String,
        name: String,
        variant: String,
        priceKopecks: Long,
        durationMinutes: Int,
        bufferMinutes: Int,
        active: Boolean
    ): ServiceItem {
        val dtoBody = ServiceDto(
            id = id ?: "",
            code = "SRV-${UUID.randomUUID().toString().take(6).uppercase()}",
            category = category,
            name = name,
            variant = variant,
            priceKopecks = priceKopecks,
            durationMinutes = durationMinutes,
            bufferMinutes = bufferMinutes,
            active = active
        )
        val dto = if (id == null) executeCall { it.createService(dtoBody) } else executeCall { it.updateService(id, dtoBody) }
        val domain = dto.toDomain()
        _services.value = _services.value.filterNot { it.id == domain.id } + domain
        return domain
    }

    override suspend fun saveWorkingWindows(date: LocalDate, windows: List<Pair<Int, Int>>) {
        val iso = date.toString()
        val body = windows.map { (s, e) -> WorkingWindowDto(day = iso, startMinute = s, endMinute = e) }
        val saved = executeCall { it.putSchedule(iso, body) }
        val mapped = saved.mapIndexed { i, w ->
            WorkingWindow(w.id.ifEmpty { "ww_${iso}_$i" }, LocalDate.parse(w.day), w.startMinute, w.endMinute)
        }
        _workingWindows.value = _workingWindows.value.filterNot { it.date == date } + mapped
    }

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
    ): CalendarEntry {
        val key = idempotencyKey ?: UUID.randomUUID().toString()
        val dto = executeCall {
            it.createAppointment(
                idempotencyKey = key,
                body = CreateAppointmentDto(
                    clientId = clientId,
                    serviceId = serviceId,
                    serviceName = serviceNameOverride,
                    source = source.code,
                    startsAt = startsAt.toString(),
                    durationMinutes = durationMinutes,
                    bufferMinutes = bufferMinutes,
                    quotedPriceKopecks = quotedPriceKopecks,
                    note = note
                )
            )
        }
        val domain = dto.toDomain()
        _calendarEntries.value = (_calendarEntries.value.filterNot { it.id == domain.id } + domain).sortedBy { it.startsAt }
        return domain
    }

    override suspend fun createBlock(startsAt: Instant, durationMinutes: Int, note: String): CalendarEntry {
        val dto = executeCall {
            it.createBlock(UUID.randomUUID().toString(), CreateBlockDto(startsAt.toString(), durationMinutes, note))
        }
        val domain = dto.toDomain()
        _calendarEntries.value = (_calendarEntries.value + domain).sortedBy { it.startsAt }
        return domain
    }

    override suspend fun moveEntry(entryId: String, newStartsAt: Instant): CalendarEntry {
        val dto = executeCall { it.moveEntry(entryId, MoveEntryDto(newStartsAt.toString())) }
        val domain = dto.toDomain()
        _calendarEntries.value = (_calendarEntries.value.filterNot { it.id == domain.id } + domain).sortedBy { it.startsAt }
        return domain
    }

    override suspend fun updateEntryStatus(entryId: String, newStatus: AppointmentStatus): CalendarEntry {
        val dto = if (newStatus == AppointmentStatus.CANCELLED) {
            executeCall { it.cancelEntry(entryId) }
        } else {
            executeCall { it.changeStatus(entryId, StatusChangeDto(newStatus.code)) }
        }
        val domain = dto.toDomain()
        _calendarEntries.value = (_calendarEntries.value.filterNot { it.id == domain.id } + domain).sortedBy { it.startsAt }
        return domain
    }

    override suspend fun softDeleteEntry(entryId: String): CalendarEntry {
        val dto = executeCall { it.softDeleteEntry(entryId) }
        val domain = dto.toDomain()
        _calendarEntries.value = _calendarEntries.value.filterNot { it.id == domain.id } + domain
        return domain
    }

    override suspend fun restoreEntry(entryId: String): CalendarEntry {
        val dto = executeCall { it.restoreEntry(entryId) }
        val domain = dto.toDomain()
        _calendarEntries.value = _calendarEntries.value.filterNot { it.id == domain.id } + domain
        return domain
    }

    override fun observeEntryEvents(entryId: String): Flow<List<CalendarEvent>> = MutableStateFlow(emptyList())

    override suspend fun addFinanceTransaction(
        kind: FinanceKind,
        category: FinanceCategory,
        amountKopecks: Long,
        occurredAt: Instant,
        note: String,
        entryId: String?,
        idempotencyKey: String?
    ): FinanceTransaction {
        val key = idempotencyKey ?: UUID.randomUUID().toString()
        val dto = executeCall {
            it.createTransaction(
                idempotencyKey = key,
                body = CreateTransactionDto(entryId, kind.code, category.code, amountKopecks, occurredAt.toString(), note)
            )
        }
        val domain = dto.toDomain()
        _transactions.value = listOf(domain) + _transactions.value.filterNot { it.id == domain.id }
        return domain
    }

    override suspend fun reverseFinanceTransaction(transactionId: String): FinanceTransaction {
        val dto = executeCall { it.reverseTransaction(transactionId) }
        val domain = dto.toDomain()
        _transactions.value = listOf(domain) + _transactions.value
        return domain
    }

    override suspend fun closeMonth(yearMonth: YearMonth): MonthClosure {
        val dto = executeCall { it.closeMonth(CloseMonthDto(yearMonth.year, yearMonth.monthValue)) }
        val domain = dto.toDomain()
        _closures.value = listOf(domain) + _closures.value
        return domain
    }

    override suspend fun reopenMonth(yearMonth: YearMonth, reason: String): MonthClosure {
        val dto = executeCall { it.reopenMonth(ReopenMonthDto(yearMonth.year, yearMonth.monthValue, reason)) }
        val domain = dto.toDomain()
        _closures.value = listOf(domain) + _closures.value.filterNot { it.id == domain.id }
        return domain
    }

    override suspend fun upsertMaterial(
        id: String?,
        name: String,
        unit: MaterialUnit,
        lowStockThresholdMilliunits: Long
    ): MaterialItem {
        val dto = executeCall {
            it.createMaterial(MaterialDto(id ?: "", name, unit.code, 0L, lowStockThresholdMilliunits))
        }
        val domain = dto.toDomain()
        _materials.value = _materials.value.filterNot { it.id == domain.id } + domain
        return domain
    }

    override suspend fun recordMaterialMovement(
        materialId: String,
        deltaMilliunits: Long,
        reason: String
    ): MaterialMovement {
        val dto = executeCall {
            it.createMovement(materialId, CreateMaterialMovementDto(deltaMilliunits, reason))
        }
        val domain = dto.toDomain()
        _movements.value = listOf(domain) + _movements.value
        _materials.value = executeCall { it.getMaterials() }.map { it.toDomain() }
        return domain
    }

    override suspend fun linkLegacyVisitToClient(legacyVisitId: String, clientId: String) {
        throw DomainException(ErrorCode.INVALID_INPUT, "Сверка архива на сервере выполняется после подтверждённого импорта")
    }
}

private fun ServiceDto.toDomain() = ServiceItem(id, code, category, name, variant, priceKopecks, durationMinutes, bufferMinutes, active)

private fun CalendarEntryDto.toDomain() = CalendarEntry(
    id = id,
    kind = AppointmentKind.fromCode(kind),
    clientId = clientId,
    clientNameSnapshot = clientName,
    serviceId = serviceId,
    serviceNameSnapshot = serviceName,
    source = AppointmentSource.fromCode(source),
    status = AppointmentStatus.fromCode(status),
    startsAt = Instant.parse(startsAt),
    endsAt = Instant.parse(endsAt),
    occupiedUntil = Instant.parse(occupiedUntil),
    durationMinutes = durationMinutes,
    bufferMinutes = bufferMinutes,
    quotedPriceKopecks = quotedPriceKopecks,
    note = note,
    deletedAt = deletedAt?.let { Instant.parse(it) }
)

private fun FinanceTransactionDto.toDomain(): FinanceTransaction {
    val k = FinanceKind.fromCode(kind)
    return FinanceTransaction(
        id = id,
        entryId = entryId,
        kind = k,
        category = FinanceCategory.fromCode(k, category),
        amountKopecks = amountKopecks,
        occurredAt = Instant.parse(occurredAt),
        note = note,
        correctionOfId = correctionOfId
    )
}

private fun MonthClosureDto.toDomain() = MonthClosure(
    id = id,
    year = year,
    month = month,
    version = version,
    incomeKopecks = incomeKopecks,
    expenseKopecks = expenseKopecks,
    balanceKopecks = balanceKopecks,
    isClosed = isClosed,
    closedAt = Instant.parse(closedAt),
    closedBy = closedBy,
    reopenedAt = reopenedAt?.let { Instant.parse(it) },
    reopenReason = reopenReason
)

private fun MaterialDto.toDomain() = MaterialItem(id, name, MaterialUnit.fromCode(unit), stockMilliunits, lowStockThresholdMilliunits)

private fun MaterialMovementDto.toDomain() = MaterialMovement(id, materialId, deltaMilliunits, reason, actor, Instant.parse(occurredAt))
