package javanepoya.ir.cloudpanel.api

import javanepoya.ir.cloudpanel.data.AccountDao
import javanepoya.ir.cloudpanel.data.AuditLogDao
import javanepoya.ir.cloudpanel.data.AuditLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZoneRepository(
    private val apiService: CloudflareApiService,
    private val accountDao: AccountDao,
    private val auditLogDao: AuditLogDao
) {
    /**
     * Fetches zones from Cloudflare API, updates local database cache for account's zone count,
     * and returns the list of zones.
     */
    suspend fun fetchZonesAndUpdateCount(accountId: Int): List<ZoneResult> = withContext(Dispatchers.IO) {
        val response = apiService.getZones()
        if (response.success) {
            val zones = response.result
            val account = accountDao.getAccountById(accountId)
            if (account != null) {
                accountDao.updateAccount(account.copy(zoneCount = zones.size))
            }
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Synchronized ${zones.size} Zones from Cloudflare API",
                    status = "Success"
                )
            )
            zones
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed to sync zones: $errorMsg",
                    status = "Failure"
                )
            )
            throw Exception("Failed to fetch zones: $errorMsg")
        }
    }

    /**
     * Purges cache for the specified zone ID.
     */
    suspend fun purgeZoneCache(
        accountId: Int,
        zoneId: String,
        all: Boolean,
        files: List<String>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val req = PurgeCacheRequest(purge_everything = all, files = files)
        val response = apiService.purgeCache(zoneId, req)
        if (response.success) {
            val logMsg = if (all) {
                "Successfully purged entire cache for zone $zoneId"
            } else {
                "Successfully purged files (${files?.size ?: 0}) for zone $zoneId"
            }
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = logMsg,
                    status = "Success"
                )
            )
            true
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown error"
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed cache purge for zone $zoneId: $errorMsg",
                    status = "Failure"
                )
            )
            throw Exception("Cache purge failed: $errorMsg")
        }
    }
}
