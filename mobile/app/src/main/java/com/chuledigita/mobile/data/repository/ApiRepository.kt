package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.HealthResponse

/**
 * Repository interface for managing backend API interactions.
 */
interface ApiRepository {
    suspend fun checkHealth(): Result<HealthResponse>
}

/**
 * Implementation of [ApiRepository] consuming Retrofit [ApiService].
 */
class ApiRepositoryImpl(
    private val apiService: ApiService
) : ApiRepository {

    override suspend fun checkHealth(): Result<HealthResponse> {
        return try {
            val response = apiService.getHealthStatus()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
