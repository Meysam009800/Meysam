package javanepoya.ir.cloudpanel.api

import javanepoya.ir.cloudpanel.data.AccountDao
import javanepoya.ir.cloudpanel.data.AuditLogDao
import javanepoya.ir.cloudpanel.data.AuditLogEntity
import javanepoya.ir.cloudpanel.data.DnsRecordDao
import javanepoya.ir.cloudpanel.data.DnsRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DnsRepository(
    private val apiService: CloudflareApiService,
    private val dnsRecordDao: DnsRecordDao,
    private val accountDao: AccountDao,
    private val auditLogDao: AuditLogDao
) {
    /**
     * Helper to resolve the Cloudflare Zone ID given a zone name.
     */
    private suspend fun resolveZoneId(zoneName: String): String {
        val response = apiService.getZones(name = zoneName)
        if (response.success && response.result.isNotEmpty()) {
            return response.result.first().id
        }
        throw Exception("Could not find Zone ID on Cloudflare for zone: $zoneName")
    }

    /**
     * Synchronizes DNS Records for a specific zone. Replaces the local DB records with the remote ones.
     */
    suspend fun syncDnsRecords(accountId: Int, zoneName: String): List<DnsRecordEntity> = withContext(Dispatchers.IO) {
        val zoneId = resolveZoneId(zoneName)
        val response = apiService.getDnsRecords(zoneId)
        
        if (response.success) {
            val remoteRecords = response.result
            
            // Clear current local records for this account
            val localRecords = dnsRecordDao.getDnsRecordsForAccount(accountId).first()
            for (local in localRecords) {
                if (local.zoneName == zoneName) {
                    dnsRecordDao.deleteDnsRecord(local)
                }
            }

            // Insert new records
            val newRecords = remoteRecords.map { remote ->
                DnsRecordEntity(
                    accountId = accountId,
                    zoneName = zoneName,
                    type = remote.type,
                    name = remote.name,
                    content = remote.content,
                    ttl = remote.ttl,
                    proxied = remote.proxied
                )
            }

            for (record in newRecords) {
                dnsRecordDao.insertDnsRecord(record)
            }

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Synchronized ${newRecords.size} DNS Records for zone $zoneName",
                    status = "Success"
                )
            )

            newRecords
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
            throw Exception("Failed to sync DNS records: $errorMsg")
        }
    }

    /**
     * Creates a new DNS Record on Cloudflare and updates the local database cache.
     */
    suspend fun createDnsRecord(
        accountId: Int,
        zoneName: String,
        type: String,
        name: String,
        content: String,
        ttl: Int,
        proxied: Boolean
    ): DnsRecordEntity = withContext(Dispatchers.IO) {
        val zoneId = resolveZoneId(zoneName)
        val request = DnsRecordRequest(type = type, name = name, content = content, ttl = ttl, proxied = proxied)
        val response = apiService.createDnsRecord(zoneId, request)

        if (response.success && response.result != null) {
            val created = DnsRecordEntity(
                accountId = accountId,
                zoneName = zoneName,
                type = response.result.type,
                name = response.result.name,
                content = response.result.content,
                ttl = response.result.ttl,
                proxied = response.result.proxied
            )
            dnsRecordDao.insertDnsRecord(created)

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Successfully created DNS Record: $type $name",
                    status = "Success"
                )
            )

            created
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
            throw Exception("Failed to create DNS record: $errorMsg")
        }
    }

    /**
     * Updates an existing DNS record on Cloudflare by finding its corresponding remote record ID,
     * executing the update API, and caching it locally.
     */
    suspend fun updateDnsRecord(
        accountId: Int,
        zoneName: String,
        localRecord: DnsRecordEntity,
        newType: String,
        newName: String,
        newContent: String,
        newTtl: Int,
        newProxied: Boolean
    ): DnsRecordEntity = withContext(Dispatchers.IO) {
        val zoneId = resolveZoneId(zoneName)
        
        // 1. Locate the remote record ID by listing and matching by original name and type
        val listResponse = apiService.getDnsRecords(zoneId, type = localRecord.type, name = localRecord.name)
        if (!listResponse.success || listResponse.result.isEmpty()) {
            throw Exception("Could not locate corresponding remote record to update on Cloudflare.")
        }
        val remoteRecordId = listResponse.result.first().id

        // 2. Perform the update request
        val request = DnsRecordRequest(type = newType, name = newName, content = newContent, ttl = newTtl, proxied = newProxied)
        val response = apiService.updateDnsRecord(zoneId, remoteRecordId, request)

        if (response.success && response.result != null) {
            val updated = localRecord.copy(
                type = response.result.type,
                name = response.result.name,
                content = response.result.content,
                ttl = response.result.ttl,
                proxied = response.result.proxied
            )
            dnsRecordDao.updateDnsRecord(updated)

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Successfully updated DNS Record: $newType $newName",
                    status = "Success"
                )
            )

            updated
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
            throw Exception("Failed to update DNS record: $errorMsg")
        }
    }

    /**
     * Deletes a DNS record on Cloudflare and clears it from the local cache.
     */
    suspend fun deleteDnsRecord(
        accountId: Int,
        zoneName: String,
        localRecord: DnsRecordEntity
    ): Boolean = withContext(Dispatchers.IO) {
        val zoneId = resolveZoneId(zoneName)
        
        // Find the remote record ID to delete
        val listResponse = apiService.getDnsRecords(zoneId, type = localRecord.type, name = localRecord.name)
        if (!listResponse.success || listResponse.result.isEmpty()) {
            // If already deleted on remote, just clean local and return true
            dnsRecordDao.deleteDnsRecord(localRecord)
            return@withContext true
        }
        val remoteRecordId = listResponse.result.first().id

        val response = apiService.deleteDnsRecord(zoneId, remoteRecordId)
        if (response.isSuccessful) {
            dnsRecordDao.deleteDnsRecord(localRecord)
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Successfully deleted DNS Record: ${localRecord.type} ${localRecord.name}",
                    status = "Success"
                )
            )
            true
        } else {
            val errorMsg = response.errorBody()?.string() ?: "HTTP error code: ${response.code()}"
            throw Exception("Failed to delete DNS record: $errorMsg")
        }
    }
}
