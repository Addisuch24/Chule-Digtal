package com.chuledigita.mobile.data.remote

import com.chuledigita.mobile.domain.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that automatically injects the active JWT Bearer authentication token
 * into the 'Authorization' header of all outgoing network requests.
 *
 * Uses synchronous cached token from SessionRepository to avoid blocking network threads.
 */
class AuthInterceptor(private val sessionRepository: SessionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Retrieve the token from in-memory cache managed by SessionRepository
        val token = sessionRepository.getCachedToken()

        val requestBuilder = originalRequest.newBuilder()

        // Inject Bearer token if it exists and Authorization header is not already specified
        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
