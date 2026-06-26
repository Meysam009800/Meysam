package javanepoya.ir.cloudpanel.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface CloudflareApiService {

    @GET("user/tokens/verify")
    suspend fun verifyToken(
        @Header("Authorization") authHeader: String
    ): TokenValidationResponse

    @GET("zones")
    suspend fun getZones(
        @Query("name") name: String? = null,
        @Query("status") status: String? = null
    ): ZoneListResponse

    @POST("zones/{zoneId}/purge_cache")
    suspend fun purgeCache(
        @Path("zoneId") zoneId: String,
        @Body request: PurgeCacheRequest
    ): PurgeCacheResponse

    @GET("zones/{zoneId}/dns_records")
    suspend fun getDnsRecords(
        @Path("zoneId") zoneId: String,
        @Query("type") type: String? = null,
        @Query("name") name: String? = null
    ): DnsListResponse

    @POST("zones/{zoneId}/dns_records")
    suspend fun createDnsRecord(
        @Path("zoneId") zoneId: String,
        @Body record: DnsRecordRequest
    ): DnsRecordSingleResponse

    @PUT("zones/{zoneId}/dns_records/{recordId}")
    suspend fun updateDnsRecord(
        @Path("zoneId") zoneId: String,
        @Path("recordId") recordId: String,
        @Body record: DnsRecordRequest
    ): DnsRecordSingleResponse

    @DELETE("zones/{zoneId}/dns_records/{recordId}")
    suspend fun deleteDnsRecord(
        @Path("zoneId") zoneId: String,
        @Path("recordId") recordId: String
    ): Response<Unit>

    @GET("accounts/{accountId}/workers/scripts")
    suspend fun getWorkers(
        @Path("accountId") accountId: String
    ): WorkerListResponse

    @Headers("Content-Type: application/javascript")
    @PUT("accounts/{accountId}/workers/scripts/{scriptName}")
    suspend fun uploadWorker(
        @Path("accountId") accountId: String,
        @Path("scriptName") scriptName: String,
        @Body scriptContent: RequestBody
    ): Response<Unit>

    @DELETE("accounts/{accountId}/workers/scripts/{scriptName}")
    suspend fun deleteWorker(
        @Path("accountId") accountId: String,
        @Path("scriptName") scriptName: String
    ): Response<Unit>

    @GET("zones/{zoneId}/analytics/dashboard")
    suspend fun getZoneAnalytics(
        @Path("zoneId") zoneId: String,
        @Query("since") since: String? = null,
        @Query("until") until: String? = null
    ): AnalyticsResponse
}
