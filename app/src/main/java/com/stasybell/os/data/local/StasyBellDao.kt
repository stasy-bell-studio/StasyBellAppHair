package com.stasybell.os.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StasyBellDao {
    // --- Sessions ---
    @Query("SELECT * FROM owner_sessions ORDER BY createdAtMs DESC")
    fun observeSessions(): Flow<List<OwnerSessionEntity>>

    @Query("SELECT * FROM owner_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): OwnerSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: OwnerSessionEntity)

    @Query("UPDATE owner_sessions SET revoked = 1 WHERE id = :id")
    suspend fun revokeSession(id: String)

    // --- Clients ---
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC")
    fun observeClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE ASC")
    suspend fun getClientsOnce(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClients(clients: List<ClientEntity>)

    // --- Services ---
    @Query("SELECT * FROM services ORDER BY category ASC, priceKopecks ASC")
    fun observeServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services")
    suspend fun getServicesOnce(): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertService(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertServices(services: List<ServiceEntity>)

    // --- Working Windows ---
    @Query("SELECT * FROM working_windows ORDER BY dateIso ASC, startMinuteOfDay ASC")
    fun observeWorkingWindows(): Flow<List<WorkingWindowEntity>>

    @Query("SELECT * FROM working_windows WHERE dateIso = :dateIso ORDER BY startMinuteOfDay ASC")
    suspend fun getWorkingWindowsForDate(dateIso: String): List<WorkingWindowEntity>

    @Query("SELECT * FROM working_windows")
    suspend fun getAllWorkingWindowsOnce(): List<WorkingWindowEntity>

    @Query("DELETE FROM working_windows WHERE dateIso = :dateIso")
    suspend fun deleteWorkingWindowsForDate(dateIso: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkingWindows(windows: List<WorkingWindowEntity>)

    // --- Calendar Entries & Events ---
    @Query("SELECT * FROM calendar_entries ORDER BY startsAtMs ASC")
    fun observeCalendarEntries(): Flow<List<CalendarEntryEntity>>

    @Query("SELECT * FROM calendar_entries ORDER BY startsAtMs ASC")
    suspend fun getCalendarEntriesOnce(): List<CalendarEntryEntity>

    @Query("SELECT * FROM calendar_entries WHERE id = :id LIMIT 1")
    suspend fun getCalendarEntryById(id: String): CalendarEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEntry(entry: CalendarEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalendarEntries(entries: List<CalendarEntryEntity>)

    @Query("SELECT * FROM calendar_events WHERE entryId = :entryId ORDER BY occurredAtMs DESC")
    fun observeEventsForEntry(entryId: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE entryId = :entryId ORDER BY occurredAtMs DESC")
    suspend fun getEventsForEntryOnce(entryId: String): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvents(events: List<CalendarEventEntity>)

    // --- Finances & Month Closures ---
    @Query("SELECT * FROM finance_transactions ORDER BY occurredAtMs DESC")
    fun observeTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY occurredAtMs DESC")
    suspend fun getTransactionsOnce(): List<FinanceTransactionEntity>

    @Query("SELECT * FROM finance_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): FinanceTransactionEntity?

    @Query("SELECT * FROM finance_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(key: String): FinanceTransactionEntity?

    @Query("SELECT * FROM finance_transactions WHERE correctionOfId = :originalId LIMIT 1")
    suspend fun getReversalOf(originalId: String): FinanceTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(tx: FinanceTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(txs: List<FinanceTransactionEntity>)

    @Query("SELECT * FROM month_closures ORDER BY year DESC, month DESC, version DESC")
    fun observeMonthClosures(): Flow<List<MonthClosureEntity>>

    @Query("SELECT * FROM month_closures WHERE year = :year AND month = :month ORDER BY version DESC")
    suspend fun getMonthClosuresFor(year: Int, month: Int): List<MonthClosureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthClosure(closure: MonthClosureEntity)

    @Update
    suspend fun updateMonthClosure(closure: MonthClosureEntity)

    // --- Materials & Movements ---
    @Query("SELECT * FROM materials ORDER BY name COLLATE NOCASE ASC")
    fun observeMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials")
    suspend fun getMaterialsOnce(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: String): MaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterial(material: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterials(materials: List<MaterialEntity>)

    @Query("SELECT * FROM material_movements ORDER BY occurredAtMs DESC")
    fun observeAllMovements(): Flow<List<MaterialMovementEntity>>

    @Query("SELECT * FROM material_movements WHERE materialId = :materialId ORDER BY occurredAtMs DESC")
    suspend fun getMovementsForMaterialOnce(materialId: String): List<MaterialMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: MaterialMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<MaterialMovementEntity>)

    // --- Legacy Archive (Strictly Isolated) ---
    @Query("SELECT * FROM legacy_visits ORDER BY visitDateIso DESC")
    fun observeLegacyVisits(): Flow<List<LegacyVisitEntity>>

    @Query("SELECT * FROM legacy_visits WHERE id = :id LIMIT 1")
    suspend fun getLegacyVisitById(id: String): LegacyVisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLegacyVisit(visit: LegacyVisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLegacyVisits(visits: List<LegacyVisitEntity>)

    @Query("SELECT * FROM legacy_monthly_summaries ORDER BY yearMonthLabel DESC")
    fun observeLegacyMonthlySummaries(): Flow<List<LegacyMonthlySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLegacyMonthlySummaries(summaries: List<LegacyMonthlySummaryEntity>)

    @Query("SELECT * FROM legacy_catalog_items ORDER BY category ASC, name ASC")
    fun observeLegacyCatalogItems(): Flow<List<LegacyCatalogItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLegacyCatalogItems(items: List<LegacyCatalogItemEntity>)
}
