package javanepoya.ir.cloudpanel

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebView

class CloudPanelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupWebViewIsolation()
    }

    private fun setupWebViewIsolation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val sharedPrefs = getSharedPreferences("cf_switcher_secure_prefs", Context.MODE_PRIVATE)
                val lastActiveAccountId = sharedPrefs.getInt("last_active_account_id", 0)
                val suffix = "account_$lastActiveAccountId"
                
                Log.d("CloudPanelApp", "Setting WebView data directory suffix to: $suffix")
                WebView.setDataDirectorySuffix(suffix)
            } catch (e: IllegalStateException) {
                Log.e("CloudPanelApp", "WebView already initialized, cannot set data directory suffix", e)
            } catch (e: Exception) {
                Log.e("CloudPanelApp", "Failed to set WebView data directory suffix", e)
            }
        } else {
            Log.d("CloudPanelApp", "SDK version is below Android P, WebView suffix separation is not supported natively.")
        }
    }
}
