package javanepoya.ir.cloudpanel.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudflareClient(private val authManager: AuthenticationManager) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        if (originalRequest.header("Authorization") != null) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val activeToken = authManager.getActiveToken()
        val newRequest = if (!activeToken.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $activeToken")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val errorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            throw NetworkException("Network communication failed: ${e.localizedMessage}", e)
        }

        when (response.code) {
            401 -> throw UnauthorizedException("Unauthorized: Invalid API token.")
            403 -> throw ForbiddenException("Forbidden: Token lacks required permissions.")
            429 -> throw RateLimitException("Rate limit exceeded: Please try again later.")
            in 400..599 -> throw ApiException("API request failed with status ${response.code}", response.code)
        }

        response
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val apiService: CloudflareApiService = Retrofit.Builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(CloudflareApiService::class.java)
}
