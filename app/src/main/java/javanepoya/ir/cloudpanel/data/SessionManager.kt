package javanepoya.ir.cloudpanel.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SessionManager(
    private val context: Context,
    private val securityManager: SecurityManager,
    private val cookieStorage: CookieStorage
) {
    private val tag = "SessionManager"
    private var currentActiveAccountId: Int = -1

    // Root directory where account-isolated sessions are kept (for pre-Android P fallback)
    private val sessionsRootDir = File(context.filesDir, "sessions")

    init {
        if (!sessionsRootDir.exists()) {
            sessionsRootDir.mkdirs()
        }
    }

    /**
     * Set the current active account ID.
     */
    fun setActiveAccountId(accountId: Int) {
        currentActiveAccountId = accountId
    }

    /**
     * Get the current active account ID.
     */
    fun getActiveAccountId(): Int {
        return currentActiveAccountId
    }

    /**
     * Returns the WebView data directory for a specific account.
     */
    private fun getWebViewDataDir(accountId: Int): File {
        val dirName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && accountId >= 0) {
            "app_webview_account_$accountId"
        } else {
            "app_webview"
        }
        return File(context.dataDir, dirName)
    }

    /**
     * Completely switches session from current active account to a target account.
     * On Android P+, this saves active preferences and restarts the process so the native
     * WebView data directory suffix applies cleanly. On Android < P, it copies files manually.
     */
    suspend fun switchSession(fromAccountId: Int, toAccountId: Int) = withContext(Dispatchers.IO) {
        Log.d(tag, "Switching session from $fromAccountId to $toAccountId")
        
        // 1. Save current active session
        if (fromAccountId > 0) {
            saveSessionInternal(fromAccountId)
        }

        // 2. Save new active account ID to SharedPreferences
        val sharedPrefs = context.getSharedPreferences("cf_switcher_secure_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("last_active_account_id", toAccountId).apply()

        // 3. If we are on Android P (API 28) or above, we use native setDataDirectorySuffix.
        // Since suffix is bound to process, switching accounts at runtime requires a quick clean process restart.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (fromAccountId > 0 && fromAccountId != toAccountId) {
                Log.d(tag, "Native WebView isolation: restarting app to apply new data directory suffix")
                withContext(Dispatchers.Main) {
                    restartApp()
                }
                return@withContext
            }
        }

        // 4. Clear current global active WebView data (Android < P fallback)
        clearGlobalWebViewStateInternal()

        // 5. Restore target session (Android < P fallback)
        if (toAccountId > 0) {
            restoreSessionInternal(toAccountId)
            currentActiveAccountId = toAccountId
        }
    }

    private fun restartApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            java.lang.System.exit(0)
        } catch (e: Exception) {
            Log.e(tag, "Failed to restart app", e)
        }
    }

    /**
     * Captures current global state (cookies, local storage) and backups for [accountId].
     */
    fun saveSession(accountId: Int) {
        Log.d(tag, "Saving session for account synchronously: $accountId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android P+ uses fully independent native directories per account suffix.
            // No manual copying/backup is required or recommended.
            Log.d(tag, "Android P+: Skipping manual directory copy as native suffix partition is active.")
            return
        }

        try {
            // Save Cookies synchronously
            val cookies = securityManager.captureWebViewSession()
            cookieStorage.saveCookies(accountId, cookies)

            // Backup WebView storage directories (Fallback for < P)
            val webviewDataDir = getWebViewDataDir(accountId)
            val cacheDir = context.cacheDir
            if (webviewDataDir.exists()) {
                val accountSessionDir = File(sessionsRootDir, "account_$accountId")
                if (accountSessionDir.exists()) {
                    deleteDirectory(accountSessionDir)
                }
                accountSessionDir.mkdirs()

                // Target directories inside app_webview
                val webviewTargets = listOf(
                    "Local Storage",
                    "Default/Local Storage",
                    "IndexedDB",
                    "Default/IndexedDB",
                    "databases",
                    "Default/databases",
                    "Web SQL",
                    "Default/Web SQL",
                    "Service Worker",
                    "Default/Service Worker",
                    "Session Storage",
                    "Default/Session Storage",
                    "GPUCache",
                    "Default/GPUCache",
                    "Cache",
                    "Default/Cache",
                    "Code Cache",
                    "Default/Code Cache",
                    "Cookies",
                    "Default/Cookies",
                    "Cookies-journal",
                    "Default/Cookies-journal"
                )

                for (path in webviewTargets) {
                    val srcDir = File(webviewDataDir, path)
                    if (srcDir.exists()) {
                        val destDir = File(accountSessionDir, "app_webview/$path")
                        copyDirectory(srcDir, destDir)
                    }
                }

                // Cache targets under context.cacheDir
                val cacheTargets = listOf(
                    "WebView",
                    "org.chromium.android_webview",
                    "app_webview"
                )
                for (path in cacheTargets) {
                    val srcDir = File(cacheDir, path)
                    if (srcDir.exists()) {
                        val destDir = File(accountSessionDir, "cache/$path")
                        copyDirectory(srcDir, destDir)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save session for account $accountId", e)
        }
    }

    private suspend fun saveSessionInternal(accountId: Int) = withContext(Dispatchers.IO) {
        Log.d(tag, "Saving session internally for account: $accountId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android P+ uses fully independent native directories per account suffix.
            // No manual copying/backup is required or recommended.
            Log.d(tag, "Android P+: Skipping manual directory copy as native suffix partition is active.")
            return@withContext
        }

        try {
            // Flush cookies first to write memory to SQLite
            withContext(Dispatchers.Main) {
                CookieManager.getInstance().flush()
            }

            // A. Capture & Save Cookies
            val cookies = securityManager.captureWebViewSession()
            cookieStorage.saveCookies(accountId, cookies)

            // B. Backup WebView storage directories (Local Storage, IndexedDB, databases, Web SQL, Cache, etc.)
            val webviewDataDir = getWebViewDataDir(accountId)
            val cacheDir = context.cacheDir
            val accountSessionDir = File(sessionsRootDir, "account_$accountId")
            
            // Delete existing backup to ensure no stale files remain
            if (accountSessionDir.exists()) {
                deleteDirectory(accountSessionDir)
            }
            accountSessionDir.mkdirs()

            if (webviewDataDir.exists()) {
                val webviewTargets = listOf(
                    "Local Storage",
                    "Default/Local Storage",
                    "IndexedDB",
                    "Default/IndexedDB",
                    "databases",
                    "Default/databases",
                    "Web SQL",
                    "Default/Web SQL",
                    "Service Worker",
                    "Default/Service Worker",
                    "Session Storage",
                    "Default/Session Storage",
                    "GPUCache",
                    "Default/GPUCache",
                    "Cache",
                    "Default/Cache",
                    "Code Cache",
                    "Default/Code Cache",
                    "Cookies",
                    "Default/Cookies",
                    "Cookies-journal",
                    "Default/Cookies-journal"
                )

                for (path in webviewTargets) {
                    val srcDir = File(webviewDataDir, path)
                    if (srcDir.exists()) {
                        val destDir = File(accountSessionDir, "app_webview/$path")
                        Log.d(tag, "Backing up WebView directory: $path")
                        copyDirectory(srcDir, destDir)
                    }
                }
            }

            if (cacheDir.exists()) {
                val cacheTargets = listOf(
                    "WebView",
                    "org.chromium.android_webview",
                    "app_webview"
                )
                for (path in cacheTargets) {
                    val srcDir = File(cacheDir, path)
                    if (srcDir.exists()) {
                        val destDir = File(accountSessionDir, "cache/$path")
                        Log.d(tag, "Backing up Cache directory: $path")
                        copyDirectory(srcDir, destDir)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save session for account $accountId", e)
        }
    }

    /**
     * Restores backed-up state (cookies, local storage) into the global space for [accountId].
     */
    fun restoreSession(accountId: Int) {
        Log.d(tag, "Restoring session synchronously for account: $accountId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android P+ uses fully independent native directories per account suffix.
            Log.d(tag, "Android P+: Skipping manual directory restore as native suffix partition is active.")
            return
        }

        try {
            val accountSessionDir = File(sessionsRootDir, "account_$accountId")
            if (accountSessionDir.exists()) {
                val webviewDataDir = getWebViewDataDir(accountId)
                val cacheDir = context.cacheDir

                // Restore app_webview target files
                val webviewBackupDir = File(accountSessionDir, "app_webview")
                if (webviewBackupDir.exists()) {
                    copyDirectory(webviewBackupDir, webviewDataDir)
                }

                // Restore cache target files
                val cacheBackupDir = File(accountSessionDir, "cache")
                if (cacheBackupDir.exists()) {
                    copyDirectory(cacheBackupDir, cacheDir)
                }
            }

            // Restore Cookies
            val cookies = cookieStorage.loadCookies(accountId)
            Handler(Looper.getMainLooper()).post {
                securityManager.switchWebViewSession(cookies)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to restore session synchronously for account $accountId", e)
        }
    }

    private suspend fun restoreSessionInternal(accountId: Int) = withContext(Dispatchers.IO) {
        Log.d(tag, "Restoring session internally for account: $accountId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android P+ uses fully independent native directories per account suffix.
            Log.d(tag, "Android P+: Skipping manual directory restore as native suffix partition is active.")
            return@withContext
        }

        try {
            val accountSessionDir = File(sessionsRootDir, "account_$accountId")
            if (accountSessionDir.exists()) {
                val webviewDataDir = getWebViewDataDir(accountId)
                val cacheDir = context.cacheDir

                // Restore WebView storage directories
                val webviewBackupDir = File(accountSessionDir, "app_webview")
                if (webviewBackupDir.exists()) {
                    copyDirectory(webviewBackupDir, webviewDataDir)
                }

                // Restore Cache directories
                val cacheBackupDir = File(accountSessionDir, "cache")
                if (cacheBackupDir.exists()) {
                    copyDirectory(cacheBackupDir, cacheDir)
                }
            }

            // Restore Cookies on the Main Thread
            val cookies = cookieStorage.loadCookies(accountId)
            withContext(Dispatchers.Main) {
                securityManager.switchWebViewSession(cookies)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to restore session internally for account $accountId", e)
        }
    }

    /**
     * Deletes isolated session storage files when an account is deleted.
     */
    fun clearSession(accountId: Int) {
        Log.d(tag, "Clearing session and cookies for account: $accountId")
        cookieStorage.clearCookies(accountId)
        
        // Delete fallback backup files
        val accountSessionDir = File(sessionsRootDir, "account_$accountId")
        if (accountSessionDir.exists()) {
            deleteDirectory(accountSessionDir)
        }

        // On Android P+, also delete the native separate WebView directory to free storage space
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val suffixDir = getWebViewDataDir(accountId)
            if (suffixDir.exists()) {
                Log.d(tag, "Deleting native WebView storage directory: ${suffixDir.absolutePath}")
                deleteDirectory(suffixDir)
            }
        }

        if (currentActiveAccountId == accountId) {
            currentActiveAccountId = -1
        }
    }

    /**
     * Clears all currently loaded/global WebView databases, local storage, and cookies.
     */
    private suspend fun clearGlobalWebViewStateInternal() = withContext(Dispatchers.IO) {
        Log.d(tag, "Clearing global WebView active state internally")
        try {
            // 1. Clear active cookies on Main Thread completely and wait for callback to confirm
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Boolean> { continuation ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                }
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
            }

            // 2. Clear physical active directories of WebView storage & Caches
            val webviewDataDir = getWebViewDataDir(currentActiveAccountId)
            if (webviewDataDir.exists()) {
                val webviewTargets = listOf(
                    "Local Storage",
                    "Default/Local Storage",
                    "IndexedDB",
                    "Default/IndexedDB",
                    "databases",
                    "Default/databases",
                    "Web SQL",
                    "Default/Web SQL",
                    "Service Worker",
                    "Default/Service Worker",
                    "Session Storage",
                    "Default/Session Storage",
                    "GPUCache",
                    "Default/GPUCache",
                    "Cache",
                    "Default/Cache",
                    "Code Cache",
                    "Default/Code Cache",
                    "Cookies",
                    "Default/Cookies",
                    "Cookies-journal",
                    "Default/Cookies-journal"
                )

                for (path in webviewTargets) {
                    val dir = File(webviewDataDir, path)
                    if (dir.exists()) {
                        Log.d(tag, "Cleaning active WebView directory: ${dir.absolutePath}")
                        if (dir.isDirectory) {
                            deleteDirectoryContent(dir)
                        } else {
                            try {
                                dir.delete()
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to delete file ${dir.absolutePath}: ${e.message}")
                            }
                        }
                    }
                }
            }

            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                val cacheTargets = listOf(
                    "WebView",
                    "org.chromium.android_webview",
                    "app_webview"
                )
                for (path in cacheTargets) {
                    val dir = File(cacheDir, path)
                    if (dir.exists()) {
                        Log.d(tag, "Cleaning active Cache directory: ${dir.absolutePath}")
                        if (dir.isDirectory) {
                            deleteDirectoryContent(dir)
                        } else {
                            try {
                                dir.delete()
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to delete file ${dir.absolutePath}: ${e.message}")
                            }
                        }
                    }
                }
            }

            // Optimize memory usage: trigger Garbage Collector to reclaim webview resources immediately
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            Log.e(tag, "Failed to clear global WebView active state internally", e)
        }
    }

    private fun clearGlobalWebViewState() {
        Log.d(tag, "Clearing global WebView active state synchronously")
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            WebStorage.getInstance().deleteAllData()

            val webviewDataDir = getWebViewDataDir(currentActiveAccountId)
            if (webviewDataDir.exists()) {
                val webviewTargets = listOf(
                    "Local Storage",
                    "Default/Local Storage",
                    "IndexedDB",
                    "Default/IndexedDB",
                    "databases",
                    "Default/databases",
                    "Web SQL",
                    "Default/Web SQL",
                    "Service Worker",
                    "Default/Service Worker",
                    "Session Storage",
                    "Default/Session Storage",
                    "GPUCache",
                    "Default/GPUCache",
                    "Cache",
                    "Default/Cache",
                    "Code Cache",
                    "Default/Code Cache",
                    "Cookies",
                    "Default/Cookies",
                    "Cookies-journal",
                    "Default/Cookies-journal"
                )

                for (path in webviewTargets) {
                    val dir = File(webviewDataDir, path)
                    if (dir.exists()) {
                        if (dir.isDirectory) {
                            deleteDirectoryContent(dir)
                        } else {
                            try {
                                dir.delete()
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to delete file ${dir.absolutePath}: ${e.message}")
                            }
                        }
                    }
                }
            }

            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                val cacheTargets = listOf(
                    "WebView",
                    "org.chromium.android_webview",
                    "app_webview"
                )
                for (path in cacheTargets) {
                    val dir = File(cacheDir, path)
                    if (dir.exists()) {
                        if (dir.isDirectory) {
                            deleteDirectoryContent(dir)
                        } else {
                            try {
                                dir.delete()
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to delete file ${dir.absolutePath}: ${e.message}")
                            }
                        }
                    }
                }
            }
            System.gc()
        } catch (e: Exception) {
            Log.e(tag, "Failed to clear global WebView active state", e)
        }
    }

    // Helper: Directory deep copy
    private fun copyDirectory(sourceLocation: File, targetLocation: File) {
        if (sourceLocation.isDirectory) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs()
            }
            val children = sourceLocation.list() ?: return
            for (child in children) {
                copyDirectory(
                    File(sourceLocation, child),
                    File(targetLocation, child)
                )
            }
        } else {
            try {
                sourceLocation.parentFile?.mkdirs()
                FileInputStream(sourceLocation).use { inStream ->
                    FileOutputStream(targetLocation).use { outStream ->
                        val buf = ByteArray(32768)
                        var len: Int
                        while (inStream.read(buf).also { len = it } > 0) {
                            outStream.write(buf, 0, len)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Failed to copy file: ${sourceLocation.absolutePath}", e)
            }
        }
    }

    // Helper: Delete whole directory
    private fun deleteDirectory(path: File): Boolean {
        if (path.exists()) {
            val files = path.listFiles() ?: return true
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not delete file ${file.absolutePath}: ${e.message}")
                    }
                }
            }
        }
        return try {
            path.delete()
        } catch (e: Exception) {
            Log.w(tag, "Could not delete directory ${path.absolutePath}: ${e.message}")
            false
        }
    }

    // Helper: Delete directory content keeping the parent dir
    private fun deleteDirectoryContent(path: File) {
        if (path.exists()) {
            val files = path.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not delete file ${file.absolutePath}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Exposes standard safe WebView destruction capability.
     */
    fun destroyWebView(webView: WebView) {
        securityManager.destroyWebView(webView)
    }
}
