package javanepoya.ir.cloudpanel.data

data class AccountProfile(
    val id: Int,
    val alias: String,
    val email: String,
    val decryptedApiToken: String,
    val workspace: String,
    val mode: String,
    val workerCount: Int,
    val zoneCount: Int,
    val lastActiveTime: Long,
    val connectionStatus: String
) {
    companion object {
        fun fromEntity(entity: AccountEntity, securityManager: SecurityManager): AccountProfile {
            val decryptedToken = if (entity.apiToken.isNotEmpty()) {
                securityManager.decrypt(entity.apiToken)
            } else {
                ""
            }
            return AccountProfile(
                id = entity.id,
                alias = entity.alias,
                email = entity.email,
                decryptedApiToken = decryptedToken,
                workspace = entity.workspace,
                mode = entity.mode,
                workerCount = entity.workerCount,
                zoneCount = entity.zoneCount,
                lastActiveTime = entity.lastActiveTime,
                connectionStatus = entity.connectionStatus
            )
        }
    }

    fun toEntity(securityManager: SecurityManager, cookies: String = ""): AccountEntity {
        val encryptedToken = if (decryptedApiToken.isNotEmpty()) {
            securityManager.encrypt(decryptedApiToken)
        } else {
            ""
        }
        return AccountEntity(
            id = id,
            alias = alias,
            email = email,
            apiToken = encryptedToken,
            workspace = workspace,
            mode = mode,
            workerCount = workerCount,
            zoneCount = zoneCount,
            lastActiveTime = lastActiveTime,
            connectionStatus = connectionStatus,
            cookies = cookies
        )
    }
}
