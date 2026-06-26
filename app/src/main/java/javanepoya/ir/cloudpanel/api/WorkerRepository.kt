package javanepoya.ir.cloudpanel.api

import javanepoya.ir.cloudpanel.data.AccountDao
import javanepoya.ir.cloudpanel.data.AuditLogDao
import javanepoya.ir.cloudpanel.data.AuditLogEntity
import javanepoya.ir.cloudpanel.data.WorkerDao
import javanepoya.ir.cloudpanel.data.WorkerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class WorkerRepository(
    private val apiService: CloudflareApiService,
    private val workerDao: WorkerDao,
    private val accountDao: AccountDao,
    private val auditLogDao: AuditLogDao
) {
    /**
     * Synchronizes workers for a specific account. Fetches scripts from Cloudflare API,
     * updates the local SQLite cache, and adjusts the user's workerCount in Room.
     */
    suspend fun syncWorkers(accountId: Int, cfAccountIdentifier: String): List<WorkerEntity> = withContext(Dispatchers.IO) {
        val response = apiService.getWorkers(cfAccountIdentifier)
        if (response.success) {
            val remoteResults = response.result
            
            // Fetch existing local workers to manage replacements
            val localWorkers = workerDao.getWorkersForAccount(accountId).first()
            
            // Delete old ones from local DB
            for (local in localWorkers) {
                workerDao.deleteWorker(local)
            }

            // Map and Insert remote ones
            val newWorkersList = remoteResults.map { remote ->
                WorkerEntity(
                    accountId = accountId,
                    name = remote.name ?: remote.id,
                    script = "// Synchronized script\n// Name: ${remote.name ?: remote.id}",
                    routes = "*",
                    status = "Active",
                    updatedAt = System.currentTimeMillis()
                )
            }

            for (worker in newWorkersList) {
                workerDao.insertWorker(worker)
            }

            // Update account's worker count
            val account = accountDao.getAccountById(accountId)
            if (account != null) {
                accountDao.updateAccount(account.copy(workerCount = newWorkersList.size))
            }

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Synchronized ${newWorkersList.size} Workers from Cloudflare API",
                    status = "Success"
                )
            )

            newWorkersList
        } else {
            val errorMsg = response.errors.firstOrNull()?.message ?: "Unknown API error"
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed to sync workers: $errorMsg",
                    status = "Failure"
                )
            )
            throw Exception("Failed to fetch workers: $errorMsg")
        }
    }

    /**
     * Deploys/Uploads a Cloudflare Worker script. If successful, updates or inserts into local cache.
     */
    suspend fun deployWorker(
        accountId: Int,
        cfAccountIdentifier: String,
        scriptName: String,
        scriptContent: String,
        existingWorkerId: Int = 0
    ): WorkerEntity = withContext(Dispatchers.IO) {
        val requestBody = scriptContent.toRequestBody("application/javascript".toMediaType())
        val response = apiService.uploadWorker(cfAccountIdentifier, scriptName, requestBody)
        
        if (response.isSuccessful) {
            val updatedWorker = WorkerEntity(
                id = existingWorkerId,
                accountId = accountId,
                name = scriptName,
                script = scriptContent,
                routes = "*",
                status = "Active",
                updatedAt = System.currentTimeMillis()
            )

            if (existingWorkerId > 0) {
                workerDao.updateWorker(updatedWorker)
            } else {
                workerDao.insertWorker(updatedWorker)
            }

            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Successfully deployed Worker: $scriptName",
                    status = "Success"
                )
            )

            updatedWorker
        } else {
            val errorMsg = response.errorBody()?.string() ?: "HTTP error code: ${response.code()}"
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed deploying Worker $scriptName: $errorMsg",
                    status = "Failure"
                )
            )
            throw Exception("Failed deploying worker script: $errorMsg")
        }
    }

    /**
     * Deletes a Cloudflare Worker script.
     */
    suspend fun deleteWorker(
        accountId: Int,
        cfAccountIdentifier: String,
        worker: WorkerEntity
    ): Boolean = withContext(Dispatchers.IO) {
        val response = apiService.deleteWorker(cfAccountIdentifier, worker.name)
        if (response.isSuccessful) {
            workerDao.deleteWorker(worker)
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Successfully deleted Worker: ${worker.name}",
                    status = "Success"
                )
            )
            true
        } else {
            val errorMsg = response.errorBody()?.string() ?: "HTTP error code: ${response.code()}"
            auditLogDao.insertAuditLog(
                AuditLogEntity(
                    accountId = accountId,
                    action = "Failed deleting Worker ${worker.name}: $errorMsg",
                    status = "Failure"
                )
            )
            throw Exception("Failed to delete worker script: $errorMsg")
        }
    }
}
