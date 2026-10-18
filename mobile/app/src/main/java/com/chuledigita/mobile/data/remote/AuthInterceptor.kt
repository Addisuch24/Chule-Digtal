package com.chuledigita.mobile.data.remote

import android.util.Log
import com.chuledigita.mobile.BuildConfig
import com.chuledigita.mobile.domain.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that automatically injects the active JWT Bearer authentication token
 * into the 'Authorization' header of all outgoing network requests.
 *
 * Uses synchronous cached token from SessionRepository to avoid blocking network threads.
 * Safely handles 401 Unauthorized responses for protected endpoints by clearing expired sessions.
 */
class AuthInterceptor(private val sessionRepository: SessionRepository) : Interceptor {

    private fun logDebug(tag: String, message: String) {
        runCatching {
            if (BuildConfig.DEBUG) Log.d(tag, message)
        }
    }

    private fun logError(tag: String, message: String) {
        runCatching {
            if (BuildConfig.DEBUG) Log.e(tag, message)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Retrieve the token from in-memory cache managed by SessionRepository
        val token = sessionRepository.getCachedToken()

        val requestBuilder = originalRequest.newBuilder()

        // Inject Bearer token if it exists and Authorization header is not already specified
        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val startTime = System.currentTimeMillis()
        try {
            val response = chain.proceed(requestBuilder.build())
            val duration = System.currentTimeMillis() - startTime

            logDebug("NetworkDiagnostic", "[HTTP ${response.code}] ${originalRequest.method} ${originalRequest.url.encodedPath} (${duration}ms)")

            // If an authenticated request (not auth login/register) returns 401 Unauthorized,
            // the session token has expired or is invalid.
            if (response.code == 401 && !originalRequest.url.encodedPath.contains("auth/")) {
                sessionRepository.clearSessionSync()
            }

            return response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logError("NetworkDiagnostic", "[NETWORK FAILURE] ${originalRequest.method} ${originalRequest.url} failed after ${duration}ms: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }
}
