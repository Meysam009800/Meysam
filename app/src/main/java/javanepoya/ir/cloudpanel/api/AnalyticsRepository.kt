package javanepoya.ir.cloudpanel.api

import javanepoya.ir.cloudpanel.data.AuditLogDao
import javanepoya.ir.cloudpanel.data.AuditLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyticsRepository(
    private val apiService: CloudflareApiService,
    private val auditLogDao: AuditLogDao
) {
    /**
     * Resolves the Cloudflare Zone ID for a given zone name.
     */
    private suspend fun resolveZoneId(zoneName: String): String {
        val response = apiService.getZones(name = zoneName)
        if (response.success && response.result.isNotEmpty()) {
            return response.result.first().id
        }
        throw Exception("Could not find Zone ID for zone: $zoneName")
    }

    /**
     * Fetches real-time traffic totals from the Cloudflare API for the selected zone name.
     */
    suspend fun getAnalyticsForZone(
        accountId: Int,
        zoneName: String,
        since: String? = null,
        until: String? = null
    ): AnalyticsTotals = withContext(Dispatchers.IO) {
        try {
            val zoneId = resolveZoneId(zoneName)
            val response = apiService.getZoneAnalytics(zoneId, since, until)
            if (response.success) {
                auditLogDao.insertAuditLog(
                    AuditLogEntity(
                        accountId = accountId,
                        action = "Retrieved real-time Analytics for $zoneName",
                        status = "Success"
                    )
                )
                response.result.totals
            } else {
                val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
                throw Exception("Failed fetching analytics: $errorMsg")
            }
        } catch (e: Exception) {
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed to fetch Analytics: ${e.localizedMessage}",
                    status = "Failure"
                )
            )
            throw e
        }
    }
}
