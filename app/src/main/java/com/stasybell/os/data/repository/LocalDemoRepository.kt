package com.stasybell.os.data.repository

import androidx.room.withTransaction
import com.stasybell.os.data.local.CalendarEntryEntity
import com.stasybell.os.data.local.CalendarEventEntity
import com.stasybell.os.data.local.ClientEntity
import com.stasybell.os.data.local.DemoSeedData
import com.stasybell.os.data.local.FinanceTransactionEntity
import com.stasybell.os.data.local.MaterialEntity
import com.stasybell.os.data.local.MaterialMovementEntity
import com.stasybell.os.data.local.MonthClosureEntity
import com.stasybell.os.data.local.OwnerSessionEntity
import com.stasybell.os.data.local.ServiceEntity
import com.stasybell.os.data.local.StasyBellDatabase
import com.stasybell.os.data.local.WorkingWindowEntity
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
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.security.LoginRateLimiter
import com.stasybell.os.security.SecureTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class LocalDemoRepository(
    private val db: StasyBellDatabase,
    private val tokenStore: SecureTokenStore,
    private val rateLimiter: LoginRateLimiter = LoginRateLimiter()
) : StasyBellRepository {

    private val dao = db.dao()
    private val _isAuthenticated = MutableStateFlow(false)

    override val dataMode: Flow<DataMode> = MutableStateFlow(DataMode.DEMO_ROOM)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated

    override val sessions: Flow<List<OwnerSession>> = dao.observeSessions().map { list ->
        val currentSid = tokenStore.getSessionId()
        list.map { it.toDomain(currentSid) }
    }

    override val clients: Flow<List<Client>> = dao.observeClients().map { list ->
        list.map { it.toDomain() }
    }

    override val services: Flow<List<ServiceItem>> = dao.observeServices().map { list ->
        list.map { it.toDomain() }
    }

    override val workingWindows: Flow<List<WorkingWindow>> = dao.observeWorkingWindows().map { list ->
        list.map { it.toDomain() }
    }

    override val calendarEntries: Flow<List<CalendarEntry>> = dao.observeCalendarEntries().map { list ->
        list.map { it.toDomain() }
    }

    override val financeTransactions: Flow<List<FinanceTransaction>> = dao.observeTransactions().map { list ->
        list.map { it.toDomain() }
    }

    override val monthClosures: Flow<List<MonthClosure>> = dao.observeMonthClosures().map { list ->
        list.map { it.toDomain() }
    }

    override val materials: Flow<List<MaterialItem>> = dao.observeMaterials().map { list ->
        list.map { it.toDomain() }
    }

    override val materialMovements: Flow<List<MaterialMovement>> = dao.observeAllMovements().map { list ->
        list.map { it.toDomain() }
    }

    override val legacyVisits: Flow<List<LegacyVisit>> = dao.observeLegacyVisits().map { list ->
        list.map { it.toDomain() }
    }

    override val legacyMonthlySummaries: Flow<List<LegacyMonthlySummary>> = dao.observeLegacyMonthlySummaries().map { list ->
        list.map { it.toDomain() }
    }

    override val legacyCatalogItems: Flow<List<LegacyCatalogItem>> = dao.observeLegacyCatalogItems().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun initialize() {
        DemoSeedData.seedIfEmpty(dao)
        restoreSessionIfValid()
    }

    override suspend fun checkServerHealth(url: String, allowDebugHttp: Boolean): Result<String> {
        return Result.success("Локальный Демо-режим Room активен")
    }

    override suspend fun switchDataMode(mode: DataMode, serverUrl: String, allowDebugHttp: Boolean) {
        tokenStore.setDataMode(mode)
    }

    override suspend fun login(username: String, password: String, deviceName: String): OwnerSession {
        val waitSec = rateLimiter.checkAllowed()
        if (waitSec != null) {
            throw DomainException(ErrorCode.RATE_LIMITED, "Слишком много попыток. Подождите $waitSec сек.")
        }
        val cleanUser = username.trim().lowercase()
        val cleanPass = password.trim()
        if (cleanUser != DEMO_USER || cleanPass != DEMO_PASS) {
            rateLimiter.recordFailure()
            throw DomainException(ErrorCode.AUTH_FAILED, "Неверный логин или пароль")
        }
        rateLimiter.recordSuccess()

        val now = Instant.now()
        val expires = now.plusSeconds(StudioRules.SESSION_TTL_DAYS * 86_400L)
        val sid = "sess_${UUID.randomUUID()}"
        val refreshRaw = "ref_${UUID.randomUUID()}"
        val accessRaw = "acc_${UUID.randomUUID()}"

        val entity = OwnerSessionEntity(
            id = sid,
            username = "Настя",
            deviceName = deviceName,
            refreshTokenHash = sha256(refreshRaw),
            createdAtMs = now.toEpochMilli(),
            expiresAtMs = expires.toEpochMilli(),
            lastUsedAtMs = now.toEpochMilli(),
            revoked = false
        )
        dao.upsertSession(entity)
        tokenStore.saveSessionTokens(sid, accessRaw, refreshRaw, expires.toEpochMilli())
        _isAuthenticated.value = true
        return entity.toDomain(sid)
    }

    override suspend fun restoreSessionIfValid(): Boolean {
        val sid = tokenStore.getSessionId()
        if (!tokenStore.hasValidSession() || sid == null) {
            _isAuthenticated.value = false
            return false
        }
        val existing = dao.getSessionById(sid)
        val valid = existing != null && !existing.revoked && existing.expiresAtMs > System.currentTimeMillis()
        if (!valid) {
            tokenStore.clearSession()
            _isAuthenticated.value = false
            return false
        }
        val updated = existing.copy(lastUsedAtMs = System.currentTimeMillis())
        dao.upsertSession(updated)
        _isAuthenticated.value = true
        return true
    }

    override suspend fun logout() {
        val sid = tokenStore.getSessionId()
        if (sid != null) {
            dao.revokeSession(sid)
        }
        tokenStore.clearSession()
        _isAuthenticated.value = false
    }

    override suspend fun revokeSession(sessionId: String) {
        dao.revokeSession(sessionId)
        if (sessionId == tokenStore.getSessionId()) {
            tokenStore.clearSession()
            _isAuthenticated.value = false
        }
    }

    override suspend fun upsertClient(
        id: String?,
        name: String,
        phone: String?,
        notes: String,
        preferredChannel: PreferredChannel
    ): Client {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите имя клиента")
        }
        val existing = id?.let { dao.getClientById(it) }
        val normalizedPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        val entity = ClientEntity(
            id = existing?.id ?: "cl_${UUID.randomUUID()}",
            name = trimmedName,
            phone = normalizedPhone,
            notes = notes.trim(),
            preferredChannelCode = preferredChannel.code,
            telegramChatId = existing?.telegramChatId,
            vkUserId = existing?.vkUserId,
            createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()
        )
        dao.upsertClient(entity)
        return entity.toDomain()
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
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите название услуги")
        }
        if (priceKopecks <= 0L) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Цена должна быть больше 0 ₽")
        }
        if (durationMinutes <= 0) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Длительность должна быть больше 0 минут")
        }
        if (bufferMinutes < 0) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Буфер не может быть отрицательным")
        }
        val existing = id?.let { dao.getServiceById(it) }
        val entity = ServiceEntity(
            id = existing?.id ?: "srv_${UUID.randomUUID()}",
            code = existing?.code ?: "SRV-${UUID.randomUUID().toString().take(6).uppercase()}",
            category = category.trim().ifEmpty { "Услуги" },
            name = trimmedName,
            variant = variant.trim(),
            priceKopecks = priceKopecks,
            durationMinutes = durationMinutes,
            bufferMinutes = bufferMinutes,
            active = active
        )
        // Updating service NEVER rewrites quotedPriceKopecks on historical calendar_entries!
        dao.upsertService(entity)
        return entity.toDomain()
    }

    override suspend fun saveWorkingWindows(date: LocalDate, windows: List<Pair<Int, Int>>) {
        val iso = date.toString()
        db.withTransaction {
            val proposed = windows.mapIndexed { idx, (start, end) ->
                WorkingWindow("ww_${iso}_$idx", date, start, end)
            }
            val allEntries = dao.getCalendarEntriesOnce().map { it.toDomain() }
            StudioRules.validateWorkingWindowsUpdate(date, proposed, allEntries)
            dao.deleteWorkingWindowsForDate(iso)
            dao.insertWorkingWindows(
                proposed.map { WorkingWindowEntity(it.id, iso, it.startMinuteOfDay, it.endMinuteOfDay) }
            )
        }
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
        return db.withTransaction {
            val client = dao.getClientById(clientId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Клиент не найден")
            val date = StudioRules.toLocalDate(startsAt)
            val windows = dao.getWorkingWindowsForDate(date.toString()).map { it.toDomain() }
            val existingEntries = dao.getCalendarEntriesOnce().map { it.toDomain() }

            StudioRules.validateSlotPlacement(
                kind = AppointmentKind.APPOINTMENT,
                startsAt = startsAt,
                durationMinutes = durationMinutes,
                bufferMinutes = bufferMinutes,
                workingWindows = windows,
                existingEntries = existingEntries
            )

            val endsAt = startsAt.plusSeconds(durationMinutes * 60L)
            val occupiedUntil = startsAt.plusSeconds((durationMinutes + bufferMinutes) * 60L)
            val entryId = "ent_${UUID.randomUUID()}"
            val entity = CalendarEntryEntity(
                id = entryId,
                kindCode = AppointmentKind.APPOINTMENT.code,
                clientId = client.id,
                clientNameSnapshot = client.name,
                serviceId = serviceId,
                serviceNameSnapshot = serviceNameOverride.trim().ifEmpty { "Услуга" },
                sourceCode = source.code,
                statusCode = AppointmentStatus.CONFIRMED.code, // Manual booking is immediately confirmed!
                startsAtMs = startsAt.toEpochMilli(),
                endsAtMs = endsAt.toEpochMilli(),
                occupiedUntilMs = occupiedUntil.toEpochMilli(),
                durationMinutes = durationMinutes,
                bufferMinutes = bufferMinutes,
                quotedPriceKopecks = quotedPriceKopecks.coerceAtLeast(0L),
                note = note.trim(),
                deletedAtMs = null
            )
            dao.upsertCalendarEntry(entity)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = entryId,
                    action = "created",
                    actor = "Настя",
                    summary = "Ручная запись подтверждена (${source.labelRu}) · ${StudioRules.formatRubles(quotedPriceKopecks)}",
                    occurredAtMs = System.currentTimeMillis()
                )
            )
            entity.toDomain()
        }
    }

    override suspend fun createBlock(
        startsAt: Instant,
        durationMinutes: Int,
        note: String
    ): CalendarEntry {
        return db.withTransaction {
            val existingEntries = dao.getCalendarEntriesOnce().map { it.toDomain() }
            val date = StudioRules.toLocalDate(startsAt)
            val windows = dao.getWorkingWindowsForDate(date.toString()).map { it.toDomain() }

            StudioRules.validateSlotPlacement(
                kind = AppointmentKind.BLOCK,
                startsAt = startsAt,
                durationMinutes = durationMinutes,
                bufferMinutes = 0,
                workingWindows = windows,
                existingEntries = existingEntries
            )
            val endsAt = startsAt.plusSeconds(durationMinutes * 60L)
            val id = "blk_${UUID.randomUUID()}"
            val entity = CalendarEntryEntity(
                id = id,
                kindCode = AppointmentKind.BLOCK.code,
                clientId = null,
                clientNameSnapshot = "Блокировка времени",
                serviceId = null,
                serviceNameSnapshot = note.trim().ifEmpty { "Перерыв / Личное время" },
                sourceCode = AppointmentSource.OTHER_MANUAL.code,
                statusCode = AppointmentStatus.BLOCKED.code,
                startsAtMs = startsAt.toEpochMilli(),
                endsAtMs = endsAt.toEpochMilli(),
                occupiedUntilMs = endsAt.toEpochMilli(),
                durationMinutes = durationMinutes,
                bufferMinutes = 0,
                quotedPriceKopecks = 0L,
                note = note.trim(),
                deletedAtMs = null
            )
            dao.upsertCalendarEntry(entity)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = id,
                    action = "blocked",
                    actor = "Настя",
                    summary = "Создана блокировка: ${entity.serviceNameSnapshot}",
                    occurredAtMs = System.currentTimeMillis()
                )
            )
            entity.toDomain()
        }
    }

    override suspend fun moveEntry(entryId: String, newStartsAt: Instant): CalendarEntry {
        return db.withTransaction {
            val existing = dao.getCalendarEntryById(entryId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Запись не найдена")
            val kind = AppointmentKind.fromCode(existing.kindCode)
            val targetDate = StudioRules.toLocalDate(newStartsAt)
            val windows = dao.getWorkingWindowsForDate(targetDate.toString()).map { it.toDomain() }
            val allEntries = dao.getCalendarEntriesOnce().map { it.toDomain() }

            StudioRules.validateSlotPlacement(
                kind = kind,
                startsAt = newStartsAt,
                durationMinutes = existing.durationMinutes,
                bufferMinutes = existing.bufferMinutes,
                workingWindows = windows,
                existingEntries = allEntries,
                excludeEntryId = entryId
            )

            val newEndsAt = newStartsAt.plusSeconds(existing.durationMinutes * 60L)
            val newOccupiedUntil = newStartsAt.plusSeconds((existing.durationMinutes + existing.bufferMinutes) * 60L)
            val updated = existing.copy(
                startsAtMs = newStartsAt.toEpochMilli(),
                endsAtMs = newEndsAt.toEpochMilli(),
                occupiedUntilMs = newOccupiedUntil.toEpochMilli()
            )
            dao.upsertCalendarEntry(updated)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = entryId,
                    action = "moved",
                    actor = "Настя",
                    summary = "Перенесено на ${StudioRules.formatDateTime(newStartsAt)}",
                    occurredAtMs = System.currentTimeMillis()
                )
            )
            updated.toDomain()
        }
    }

    override suspend fun updateEntryStatus(entryId: String, newStatus: AppointmentStatus): CalendarEntry {
        return db.withTransaction {
            val existing = dao.getCalendarEntryById(entryId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Запись не найдена")
            // CRITICAL INVARIANT: Changing status to COMPLETED NEVER auto-creates a cash transaction!
            val updated = existing.copy(statusCode = newStatus.code)
            dao.upsertCalendarEntry(updated)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = entryId,
                    action = "status_changed",
                    actor = "Настя",
                    summary = "Статус изменён на «${newStatus.labelRu}»",
                    occurredAtMs = System.currentTimeMillis()
                )
            )
            updated.toDomain()
        }
    }

    override suspend fun softDeleteEntry(entryId: String): CalendarEntry {
        return db.withTransaction {
            val existing = dao.getCalendarEntryById(entryId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Запись не найдена")
            val nowMs = System.currentTimeMillis()
            val updated = existing.copy(deletedAtMs = nowMs)
            dao.upsertCalendarEntry(updated)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = entryId,
                    action = "deleted",
                    actor = "Настя",
                    summary = "Запись удалена (мягкое удаление)",
                    occurredAtMs = nowMs
                )
            )
            updated.toDomain()
        }
    }

    override suspend fun restoreEntry(entryId: String): CalendarEntry {
        return db.withTransaction {
            val existing = dao.getCalendarEntryById(entryId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Запись не найдена")
            val startsAt = Instant.ofEpochMilli(existing.startsAtMs)
            val date = StudioRules.toLocalDate(startsAt)
            val windows = dao.getWorkingWindowsForDate(date.toString()).map { it.toDomain() }
            val allEntries = dao.getCalendarEntriesOnce().map { it.toDomain() }

            StudioRules.validateSlotPlacement(
                kind = AppointmentKind.fromCode(existing.kindCode),
                startsAt = startsAt,
                durationMinutes = existing.durationMinutes,
                bufferMinutes = existing.bufferMinutes,
                workingWindows = windows,
                existingEntries = allEntries,
                excludeEntryId = entryId
            )

            val updated = existing.copy(deletedAtMs = null)
            dao.upsertCalendarEntry(updated)
            dao.insertCalendarEvent(
                CalendarEventEntity(
                    id = "ev_${UUID.randomUUID()}",
                    entryId = entryId,
                    action = "restored",
                    actor = "Настя",
                    summary = "Запись восстановлена в календаре",
                    occurredAtMs = System.currentTimeMillis()
                )
            )
            updated.toDomain()
        }
    }

    override fun observeEntryEvents(entryId: String): Flow<List<CalendarEvent>> =
        dao.observeEventsForEntry(entryId).map { list -> list.map { it.toDomain() } }

    override suspend fun addFinanceTransaction(
        kind: FinanceKind,
        category: FinanceCategory,
        amountKopecks: Long,
        occurredAt: Instant,
        note: String,
        entryId: String?,
        idempotencyKey: String?
    ): FinanceTransaction {
        if (amountKopecks <= 0L) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Сумма операции должна быть больше 0 ₽")
        }
        return db.withTransaction {
            if (!idempotencyKey.isNullOrBlank()) {
                val existingByKey = dao.getTransactionByIdempotencyKey(idempotencyKey)
                if (existingByKey != null) {
                    return@withTransaction existingByKey.toDomain()
                }
            }

            val ym = StudioRules.toYearMonth(occurredAt)
            val closures = dao.getMonthClosuresFor(ym.year, ym.monthValue)
            if (closures.firstOrNull()?.isClosed == true) {
                throw DomainException(ErrorCode.MONTH_CLOSED, "Месяц закрыт. Переоткройте месяц для внесения правки.")
            }

            val entity = FinanceTransactionEntity(
                id = "tx_${UUID.randomUUID()}",
                entryId = entryId,
                kindCode = kind.code,
                categoryCode = category.code,
                amountKopecks = amountKopecks,
                occurredAtMs = occurredAt.toEpochMilli(),
                note = note.trim(),
                correctionOfId = null,
                idempotencyKey = idempotencyKey
            )
            dao.insertTransaction(entity)
            entity.toDomain()
        }
    }

    override suspend fun reverseFinanceTransaction(transactionId: String): FinanceTransaction {
        return db.withTransaction {
            val original = dao.getTransactionById(transactionId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Операция не найдена")
            if (original.correctionOfId != null) {
                throw DomainException(ErrorCode.ALREADY_REVERSED, "Нельзя сторнировать запись-сторно")
            }
            if (dao.getReversalOf(transactionId) != null) {
                throw DomainException(ErrorCode.ALREADY_REVERSED, "Операция уже сторнирована")
            }
            val ym = StudioRules.toYearMonth(Instant.ofEpochMilli(original.occurredAtMs))
            val closures = dao.getMonthClosuresFor(ym.year, ym.monthValue)
            if (closures.firstOrNull()?.isClosed == true) {
                throw DomainException(ErrorCode.MONTH_CLOSED, "Месяц закрыт. Переоткройте месяц для сторнирования.")
            }

            val reversal = FinanceTransactionEntity(
                id = "tx_rev_${UUID.randomUUID()}",
                entryId = original.entryId,
                kindCode = original.kindCode,
                categoryCode = original.categoryCode,
                amountKopecks = -original.amountKopecks,
                occurredAtMs = original.occurredAtMs,
                note = "Сторно: ${original.note.ifEmpty { "операция ${original.id.takeLast(6)}" }}",
                correctionOfId = original.id,
                idempotencyKey = "rev_${original.id}"
            )
            dao.insertTransaction(reversal)
            reversal.toDomain()
        }
    }

    override suspend fun closeMonth(yearMonth: YearMonth): MonthClosure {
        return db.withTransaction {
            val existing = dao.getMonthClosuresFor(yearMonth.year, yearMonth.monthValue)
            if (existing.firstOrNull()?.isClosed == true) {
                throw DomainException(ErrorCode.MONTH_CLOSED, "Месяц уже закрыт")
            }
            val allTx = dao.getTransactionsOnce().map { it.toDomain() }
            val monthTx = allTx.filter { StudioRules.toYearMonth(it.occurredAt) == yearMonth }
            val income = monthTx.filter { it.kind == FinanceKind.INCOME }.sumOf { it.amountKopecks }
            val expense = monthTx.filter { it.kind == FinanceKind.EXPENSE }.sumOf { it.amountKopecks }
            val nextVersion = (existing.maxOfOrNull { it.version } ?: 0) + 1

            val entity = MonthClosureEntity(
                id = "cl_${yearMonth.year}_${yearMonth.monthValue}_v$nextVersion",
                year = yearMonth.year,
                month = yearMonth.monthValue,
                version = nextVersion,
                incomeKopecks = income,
                expenseKopecks = expense,
                balanceKopecks = income - expense,
                isClosed = true,
                closedAtMs = System.currentTimeMillis(),
                closedBy = "Настя",
                reopenedAtMs = null,
                reopenReason = null
            )
            dao.upsertMonthClosure(entity)
            entity.toDomain()
        }
    }

    override suspend fun reopenMonth(yearMonth: YearMonth, reason: String): MonthClosure {
        val cleanReason = reason.trim()
        if (cleanReason.length < 3) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите причину переоткрытия месяца")
        }
        return db.withTransaction {
            val latest = dao.getMonthClosuresFor(yearMonth.year, yearMonth.monthValue).firstOrNull()
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Закрытие месяца не найдено")
            if (!latest.isClosed) {
                throw DomainException(ErrorCode.INVALID_INPUT, "Месяц уже открыт")
            }
            val updated = latest.copy(
                isClosed = false,
                reopenedAtMs = System.currentTimeMillis(),
                reopenReason = cleanReason
            )
            dao.updateMonthClosure(updated)
            updated.toDomain()
        }
    }

    override suspend fun upsertMaterial(
        id: String?,
        name: String,
        unit: MaterialUnit,
        lowStockThresholdMilliunits: Long
    ): MaterialItem {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите название материала")
        }
        val existing = id?.let { dao.getMaterialById(it) }
        val entity = MaterialEntity(
            id = existing?.id ?: "mat_${UUID.randomUUID()}",
            name = trimmed,
            unitCode = unit.code,
            stockMilliunits = existing?.stockMilliunits ?: 0L,
            lowStockThresholdMilliunits = lowStockThresholdMilliunits.coerceAtLeast(0L)
        )
        dao.upsertMaterial(entity)
        return entity.toDomain()
    }

    override suspend fun recordMaterialMovement(
        materialId: String,
        deltaMilliunits: Long,
        reason: String
    ): MaterialMovement {
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty()) {
            throw DomainException(ErrorCode.INVALID_INPUT, "Укажите причину движения по складу")
        }
        return db.withTransaction {
            val mat = dao.getMaterialById(materialId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Материал не найден")
            val nextStock = StudioRules.validateStockMovement(mat.stockMilliunits, deltaMilliunits)
            dao.upsertMaterial(mat.copy(stockMilliunits = nextStock))

            // CRITICAL INVARIANT: Material receipt/deduction NEVER creates a finance transaction!
            val movement = MaterialMovementEntity(
                id = "mov_${UUID.randomUUID()}",
                materialId = materialId,
                deltaMilliunits = deltaMilliunits,
                reason = cleanReason,
                actor = "Настя",
                occurredAtMs = System.currentTimeMillis()
            )
            dao.insertMovement(movement)
            movement.toDomain()
        }
    }

    override suspend fun linkLegacyVisitToClient(legacyVisitId: String, clientId: String) {
        db.withTransaction {
            val visit = dao.getLegacyVisitById(legacyVisitId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Архивная запись не найдена")
            val client = dao.getClientById(clientId)
                ?: throw DomainException(ErrorCode.NOT_FOUND, "Клиент не найден")
            dao.upsertLegacyVisit(
                visit.copy(
                    clientId = client.id,
                    isUnmatched = false
                )
            )
        }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val DEMO_USER = "nastya"
        const val DEMO_PASS = "stasy2026"
    }
}

private fun OwnerSessionEntity.toDomain(currentSid: String?) = OwnerSession(
    id = id,
    username = username,
    deviceName = deviceName,
    createdAt = Instant.ofEpochMilli(createdAtMs),
    expiresAt = Instant.ofEpochMilli(expiresAtMs),
    lastUsedAt = Instant.ofEpochMilli(lastUsedAtMs),
    revoked = revoked,
    isCurrent = id == currentSid
)

private fun ClientEntity.toDomain() = Client(
    id = id,
    name = name,
    phone = phone,
    notes = notes,
    preferredChannel = PreferredChannel.fromCode(preferredChannelCode),
    telegramChatId = telegramChatId,
    vkUserId = vkUserId,
    createdAt = Instant.ofEpochMilli(createdAtMs)
)

private fun ServiceEntity.toDomain() = ServiceItem(
    id = id,
    code = code,
    category = category,
    name = name,
    variant = variant,
    priceKopecks = priceKopecks,
    durationMinutes = durationMinutes,
    bufferMinutes = bufferMinutes,
    active = active
)

private fun WorkingWindowEntity.toDomain() = WorkingWindow(
    id = id,
    date = LocalDate.parse(dateIso),
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay
)

private fun CalendarEntryEntity.toDomain() = CalendarEntry(
    id = id,
    kind = AppointmentKind.fromCode(kindCode),
    clientId = clientId,
    clientNameSnapshot = clientNameSnapshot,
    serviceId = serviceId,
    serviceNameSnapshot = serviceNameSnapshot,
    source = AppointmentSource.fromCode(sourceCode),
    status = AppointmentStatus.fromCode(statusCode),
    startsAt = Instant.ofEpochMilli(startsAtMs),
    endsAt = Instant.ofEpochMilli(endsAtMs),
    occupiedUntil = Instant.ofEpochMilli(occupiedUntilMs),
    durationMinutes = durationMinutes,
    bufferMinutes = bufferMinutes,
    quotedPriceKopecks = quotedPriceKopecks,
    note = note,
    deletedAt = deletedAtMs?.let { Instant.ofEpochMilli(it) }
)

private fun CalendarEventEntity.toDomain() = CalendarEvent(
    id = id,
    entryId = entryId,
    action = action,
    actor = actor,
    summary = summary,
    occurredAt = Instant.ofEpochMilli(occurredAtMs)
)

private fun FinanceTransactionEntity.toDomain(): FinanceTransaction {
    val kind = FinanceKind.fromCode(kindCode)
    return FinanceTransaction(
        id = id,
        entryId = entryId,
        kind = kind,
        category = FinanceCategory.fromCode(kind, categoryCode),
        amountKopecks = amountKopecks,
        occurredAt = Instant.ofEpochMilli(occurredAtMs),
        note = note,
        correctionOfId = correctionOfId,
        idempotencyKey = idempotencyKey
    )
}

private fun MonthClosureEntity.toDomain() = MonthClosure(
    id = id,
    year = year,
    month = month,
    version = version,
    incomeKopecks = incomeKopecks,
    expenseKopecks = expenseKopecks,
    balanceKopecks = balanceKopecks,
    isClosed = isClosed,
    closedAt = Instant.ofEpochMilli(closedAtMs),
    closedBy = closedBy,
    reopenedAt = reopenedAtMs?.let { Instant.ofEpochMilli(it) },
    reopenReason = reopenReason
)

private fun MaterialEntity.toDomain() = MaterialItem(
    id = id,
    name = name,
    unit = MaterialUnit.fromCode(unitCode),
    stockMilliunits = stockMilliunits,
    lowStockThresholdMilliunits = lowStockThresholdMilliunits
)

private fun MaterialMovementEntity.toDomain() = MaterialMovement(
    id = id,
    materialId = materialId,
    deltaMilliunits = deltaMilliunits,
    reason = reason,
    actor = actor,
    occurredAt = Instant.ofEpochMilli(occurredAtMs)
)

private fun com.stasybell.os.data.local.LegacyVisitEntity.toDomain() = LegacyVisit(
    id = id,
    sourceRef = sourceRef,
    visitDate = LocalDate.parse(visitDateIso),
    serviceTitle = serviceTitle,
    amountOriginKopecks = amountOriginKopecks,
    sourceFile = sourceFile,
    sourceSheet = sourceSheet,
    sourceRow = sourceRow,
    clientId = clientId,
    clientRawName = clientRawName,
    isUnmatched = isUnmatched
)

private fun com.stasybell.os.data.local.LegacyMonthlySummaryEntity.toDomain() = LegacyMonthlySummary(
    id = id,
    yearMonthLabel = yearMonthLabel,
    sourcePaidKopecks = sourcePaidKopecks,
    sourceCostKopecks = sourceCostKopecks,
    uniqueClients = uniqueClients,
    visitsCount = visitsCount,
    note = note
)

private fun com.stasybell.os.data.local.LegacyCatalogItemEntity.toDomain() = LegacyCatalogItem(
    id = id,
    sourceRef = sourceRef,
    name = name,
    category = category,
    sourcePriceKopecks = sourcePriceKopecks
)
