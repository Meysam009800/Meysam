package javanepoya.ir.cloudpanel.api

import android.content.Context
import javanepoya.ir.cloudpanel.data.AppDatabase
import javanepoya.ir.cloudpanel.data.SecurityManager
import javanepoya.ir.cloudpanel.data.SessionManager
import kotlinx.coroutines.runBlocking

class AuthenticationManager(
    private val context: Context,
    private val securityManager: SecurityManager,
    private val sessionManager: SessionManager
) {
    private val accountDao = AppDatabase.getDatabase(context).accountDao()

    /**
     * Dynamically loads the decrypted API token for the currently active account.
     */
    fun getActiveToken(): String? {
        val activeAccountId = sessionManager.getActiveAccountId()
        if (activeAccountId <= 0) return null
        
        return runBlocking {
            try {
                val account = accountDao.getAccountById(activeAccountId)
                if (account != null && account.apiToken.isNotEmpty()) {
                    securityManager.decrypt(account.apiToken)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
