package javanepoya.ir.cloudpanel.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class TokenValidator(private val apiService: CloudflareApiService) {

    private val tag = "TokenValidator"

    /**
     * Validates a Cloudflare API token against the verify endpoint.
     * Returns true if active, or false/throws exception if invalid.
     */
    suspend fun validate(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val authHeader = "Bearer $token"
            val response = apiService.verifyToken(authHeader)
            val isValid = response.success && response.result?.status == "active"
            Log.d(tag, "Token validation result: $isValid")
            isValid
        } catch (e: UnauthorizedException) {
            Log.e(tag, "Token validation unauthorized: ${e.message}")
            false
        } catch (e: ForbiddenException) {
            Log.e(tag, "Token validation forbidden: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(tag, "Token validation failed: ${e.message}")
            false
        }
    }
}
