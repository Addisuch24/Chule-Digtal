package com.chuledigita.mobile.data.remote

import com.chuledigita.mobile.BuildConfig
import com.chuledigita.mobile.domain.repository.SessionRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton factory for configuring OkHttpClient and Retrofit API Service.
 */
object RetrofitClient {

    /**
     * Builds and returns OkHttpClient configured with authentication and logging interceptors.
     */
    private fun createOkHttpClient(sessionRepository: SessionRepository): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Add automatic JWT header injection interceptor using cached token
            .addInterceptor(AuthInterceptor(sessionRepository))

        // Enable HTTP body logging in debug mode
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Creates and returns configured [ApiService] instance.
     */
    fun createApiService(sessionRepository: SessionRepository, baseUrl: String = ApiConfig.BASE_URL): ApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(sessionRepository))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
