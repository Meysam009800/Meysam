package javanepoya.ir.cloudpanel.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiError(
    val code: Int,
    val message: String
)

@JsonClass(generateAdapter = true)
data class TokenValidationResponse(
    val result: TokenValidationResult?,
    val success: Boolean,
    val errors: List<ApiError>,
    val messages: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TokenValidationResult(
    val id: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class ZoneListResponse(
    val result: List<ZoneResult>,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class ZoneResult(
    val id: String,
    val name: String,
    val status: String,
    val paused: Boolean,
    val type: String
)

@JsonClass(generateAdapter = true)
data class PurgeCacheRequest(
    val purge_everything: Boolean = true,
    val files: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class PurgeCacheResponse(
    val result: PurgeCacheResult?,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class PurgeCacheResult(
    val id: String
)

@JsonClass(generateAdapter = true)
data class DnsListResponse(
    val result: List<DnsRecordResult>,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class DnsRecordResult(
    val id: String,
    val zone_id: String,
    val zone_name: String,
    val name: String,
    val type: String,
    val content: String,
    val ttl: Int,
    val proxied: Boolean
)

@JsonClass(generateAdapter = true)
data class DnsRecordRequest(
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int,
    val proxied: Boolean
)

@JsonClass(generateAdapter = true)
data class DnsRecordSingleResponse(
    val result: DnsRecordResult?,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class WorkerListResponse(
    val result: List<WorkerResult>,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class WorkerResult(
    val id: String,
    val name: String?,
    val created_on: String?,
    val modified_on: String?
)

@JsonClass(generateAdapter = true)
data class AnalyticsResponse(
    val result: AnalyticsResult,
    val success: Boolean,
    val errors: List<ApiError>
)

@JsonClass(generateAdapter = true)
data class AnalyticsResult(
    val totals: AnalyticsTotals
)

@JsonClass(generateAdapter = true)
data class AnalyticsTotals(
    val requests: Long,
    val bandwidth: Long,
    val threats: Long,
    val pageviews: Long
)
