package javanepoya.ir.cloudpanel.data

import android.content.Context
import android.content.SharedPreferences

class CookieStorage(private val context: Context, private val securityManager: SecurityManager) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("cf_cookie_secure_storage", Context.MODE_PRIVATE)

    /**
     * Saves cookies securely using Keystore encryption.
     */
    fun saveCookies(accountId: Int, cookies: String) {
        if (cookies.isEmpty()) {
            sharedPrefs.edit().remove("cookies_$accountId").apply()
            return
        }
        val encryptedCookies = securityManager.encrypt(cookies)
        sharedPrefs.edit().putString("cookies_$accountId", encryptedCookies).apply()
    }

    /**
     * Loads cookies securely and decrypts them using Keystore.
     */
    fun loadCookies(accountId: Int): String {
        val encryptedCookies = sharedPrefs.getString("cookies_$accountId", "") ?: return ""
        if (encryptedCookies.isEmpty()) return ""
        return securityManager.decrypt(encryptedCookies)
    }

    /**
     * Clears cookies for the account.
     */
    fun clearCookies(accountId: Int) {
        sharedPrefs.edit().remove("cookies_$accountId").apply()
    }
}
