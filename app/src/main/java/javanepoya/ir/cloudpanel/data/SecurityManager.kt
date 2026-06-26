package javanepoya.ir.cloudpanel.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("cf_switcher_secure_prefs", Context.MODE_PRIVATE)
    private val keyStoreAlias = "CfSwitcherSecKey"

    init {
        initKeystoreKey()
    }

    // Initialize Secret Key in Android Keystore
    private fun initKeystoreKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyStoreAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    keyStoreAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Encrypt sensitive Cloudflare tokens using AES/GCM
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = keyStore.getKey(keyStoreAlias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            
            // Combine IV and CipherText
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // Decrypt sensitive Cloudflare tokens using AES/GCM
    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val secretKey = keyStore.getKey(keyStoreAlias, null) as SecretKey
            val combined = Base64.decode(cipherText, Base64.DEFAULT)
            
            val iv = ByteArray(12) // AES GCM standard IV size is 12 bytes
            val encryptedBytes = ByteArray(combined.size - iv.size)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // PIN lock management
    fun setPin(pin: String) {
        val encryptedPin = encrypt(pin)
        sharedPrefs.edit().putString("app_pin", encryptedPin).apply()
    }

    fun hasPin(): Boolean {
        return !sharedPrefs.getString("app_pin", "").isNullOrEmpty()
    }

    fun verifyPin(pin: String): Boolean {
        val storedEncryptedPin = sharedPrefs.getString("app_pin", "") ?: return false
        val decryptedPin = decrypt(storedEncryptedPin)
        return pin == decryptedPin
    }

    fun clearPin() {
        sharedPrefs.edit().remove("app_pin").apply()
    }

    // Biometric enablement status
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPrefs.getBoolean("biometric_enabled", false)
    }

    // Auto lock management
    fun setAutoLockDuration(minutes: Int) {
        sharedPrefs.edit().putInt("auto_lock_duration", minutes).apply()
    }

    fun getAutoLockDuration(): Int {
        return sharedPrefs.getInt("auto_lock_duration", 5) // Default 5 minutes
    }

    // App Language configuration ("en" for English, "fa" for Persian)
    fun setLanguage(language: String) {
        sharedPrefs.edit().putString("app_language", language).apply()
    }

    fun getLanguage(): String {
        return sharedPrefs.getString("app_language", "en") ?: "en"
    }

    // WebView Multi-Account Session Isolation
    // Clears standard web storage and loads the specified cookie string
    fun switchWebViewSession(accountCookies: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        // Clear local storage / WebStorage for isolated context
        WebStorage.getInstance().deleteAllData()

        if (accountCookies.isNotEmpty()) {
            val cookiesArray = accountCookies.split("||")
            for (cookie in cookiesArray) {
                if (cookie.trim().isNotEmpty()) {
                    // Inject cookies back into cloudflare domains
                    cookieManager.setCookie("https://dash.cloudflare.com", cookie)
                    cookieManager.setCookie("https://cloudflare.com", cookie)
                }
            }
            cookieManager.flush()
        }
    }

    // Extracts active WebView cookies for active domain
    fun captureWebViewSession(): String {
        val cookieManager = CookieManager.getInstance()
        val dashCookies = cookieManager.getCookie("https://dash.cloudflare.com") ?: ""
        val mainCookies = cookieManager.getCookie("https://cloudflare.com") ?: ""
        
        val combined = mutableListOf<String>()
        if (dashCookies.isNotEmpty()) combined.addAll(dashCookies.split(";"))
        if (mainCookies.isNotEmpty()) combined.addAll(mainCookies.split(";"))
        
        return combined.distinct().joinToString("||")
    }

    /**
     * Safely tears down and destroys a WebView instance on the Main Thread to prevent memory leaks,
     * release thread resources, and unlock any stored database files before session changes.
     */
    fun destroyWebView(webView: WebView) {
        val destroyRunnable = Runnable {
            try {
                webView.loadUrl("about:blank")
                webView.stopLoading()
                try {
                    webView.removeJavascriptInterface("Android")
                    android.util.Log.d("SecurityManager", "Removed 'Android' Javascript interface from WebView")
                } catch (e: Exception) {
                    android.util.Log.w("SecurityManager", "Failed to remove 'Android' Javascript interface", e)
                }
                webView.clearHistory()
                webView.clearCache(true)
                webView.onPause()
                
                // Remove from parent view to ensure it can be garbage collected
                val parent = webView.parent
                if (parent is android.view.ViewGroup) {
                    parent.removeView(webView)
                }
                
                webView.removeAllViews()
                webView.destroy()
                android.util.Log.d("SecurityManager", "WebView instance successfully destroyed on main thread")
                
                // Explicitly request garbage collection to reclaim resources immediately
                System.gc()
                Runtime.getRuntime().gc()
            } catch (e: Exception) {
                android.util.Log.e("SecurityManager", "Failed to destroy WebView instance safely", e)
            }
        }

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            destroyRunnable.run()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(destroyRunnable)
        }
    }
}
