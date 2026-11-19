package com.stasybell.os.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Typed Retrofit contract compatible with the FastAPI + PostgreSQL backend (03_DATA_AND_API.md).
 */
interface StasyBellApiService {

    @GET("health")
    suspend fun health(): Response<HealthDto>

    // --- Auth & Owner Sessions ---
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<TokenResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<TokenResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("owner/sessions")
    suspend fun getSessions(): Response<List<SessionDto>>

    @POST("owner/sessions/{id}/revoke")
    suspend fun revokeSession(@Path("id") id: String): Response<Unit>

    // --- Clients ---
    @GET("owner/clients")
    suspend fun getClients(): Response<List<ClientDto>>

    @POST("owner/clients")
    suspend fun createClient(@Body body: CreateClientDto): Response<ClientDto>

    @PUT("owner/clients/{id}")
    suspend fun updateClient(@Path("id") id: String, @Body body: CreateClientDto): Response<ClientDto>

    // --- Services ---
    @GET("owner/services")
    suspend fun getServices(): Response<List<ServiceDto>>

    @POST("owner/services")
    suspend fun createService(@Body body: ServiceDto): Response<ServiceDto>

    @PUT("owner/services/{id}")
    suspend fun updateService(@Path("id") id: String, @Body body: ServiceDto): Response<ServiceDto>

    // --- Working Schedule ---
    @GET("owner/schedule/{day}")
    suspend fun getSchedule(@Path("day") dayIso: String): Response<List<WorkingWindowDto>>

    @PUT("owner/schedule/{day}")
    suspend fun putSchedule(
        @Path("day") dayIso: String,
        @Body windows: List<WorkingWindowDto>
    ): Response<List<WorkingWindowDto>>

    // --- Calendar ---
    @GET("owner/calendar")
    suspend fun getCalendar(
        @Query("from") fromIso: String? = null,
        @Query("to") toIso: String? = null
    ): Response<List<CalendarEntryDto>>

    @POST("owner/calendar/appointments")
    suspend fun createAppointment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateAppointmentDto
    ): Response<CalendarEntryDto>

    @POST("owner/calendar/blocks")
    suspend fun createBlock(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateBlockDto
    ): Response<CalendarEntryDto>

    @PATCH("owner/calendar/{id}/move")
    suspend fun moveEntry(
        @Path("id") id: String,
        @Body body: MoveEntryDto
    ): Response<CalendarEntryDto>

    @POST("owner/calendar/{id}/cancel")
    suspend fun cancelEntry(@Path("id") id: String): Response<CalendarEntryDto>

    @POST("owner/calendar/{id}/status")
    suspend fun changeStatus(
        @Path("id") id: String,
        @Body body: StatusChangeDto
    ): Response<CalendarEntryDto>

    @POST("owner/calendar/{id}/delete")
    suspend fun softDeleteEntry(@Path("id") id: String): Response<CalendarEntryDto>

    @POST("owner/calendar/{id}/restore")
    suspend fun restoreEntry(@Path("id") id: String): Response<CalendarEntryDto>

    @GET("owner/calendar/{id}/events")
    suspend fun getEntryEvents(@Path("id") id: String): Response<List<CalendarEventDto>>

    // --- Finances (Manual Actual Cash Ledger) ---
    @GET("owner/finances/transactions")
    suspend fun getTransactions(): Response<List<FinanceTransactionDto>>

    @POST("owner/finances/transactions")
    suspend fun createTransaction(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateTransactionDto
    ): Response<FinanceTransactionDto>

    @POST("owner/finances/transactions/{id}/reverse")
    suspend fun reverseTransaction(@Path("id") id: String): Response<FinanceTransactionDto>

    @GET("owner/finances/closures")
    suspend fun getClosures(): Response<List<MonthClosureDto>>

    @POST("owner/finances/close")
    suspend fun closeMonth(@Body body: CloseMonthDto): Response<MonthClosureDto>

    @POST("owner/finances/reopen")
    suspend fun reopenMonth(@Body body: ReopenMonthDto): Response<MonthClosureDto>

    // --- Materials & Stock ---
    @GET("owner/materials")
    suspend fun getMaterials(): Response<List<MaterialDto>>

    @POST("owner/materials")
    suspend fun createMaterial(@Body body: MaterialDto): Response<MaterialDto>

    @POST("owner/materials/{id}/movement")
    suspend fun createMovement(
        @Path("id") id: String,
        @Body body: CreateMaterialMovementDto
    ): Response<MaterialMovementDto>

    @GET("owner/materials/{id}/movements")
    suspend fun getMovements(@Path("id") id: String): Response<List<MaterialMovementDto>>
}
