package com.chuledigita.mobile

import android.content.Context
import com.chuledigita.mobile.data.local.TokenStorage
import com.chuledigita.mobile.data.remote.ApiConfig
import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.RetrofitClient
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.repository.ApiRepository
import com.chuledigita.mobile.data.repository.ApiRepositoryImpl
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.data.repository.FinancialRepositoryImpl
import com.chuledigita.mobile.data.repository.NoteRepositoryImpl
import com.chuledigita.mobile.data.repository.NotificationRepositoryImpl
import com.chuledigita.mobile.data.repository.OrderRepositoryImpl
import com.chuledigita.mobile.data.repository.SessionRepositoryImpl
import com.chuledigita.mobile.domain.repository.AuthRepository
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.domain.repository.NoteRepository
import com.chuledigita.mobile.domain.repository.NotificationRepository
import com.chuledigita.mobile.domain.repository.OrderRepository
import com.chuledigita.mobile.domain.repository.SessionRepository

/**
 * AppContainer provides manual dependency injection for the app.
 */
interface AppContainer {
    val tokenStorage: TokenStorage
    val sessionRepository: SessionRepository
    val authRepository: AuthRepository
    val apiService: ApiService
    val apiRepository: ApiRepository
    val customerRepository: CustomerRepository
    val orderRepository: OrderRepository
    val noteRepository: NoteRepository
    val socketManager: SocketManager
    val notificationRepository: NotificationRepository
    val financialRepository: FinancialRepository
}

/**
 * Default implementation of [AppContainer] managing singleton repository instances.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val tokenStorage: TokenStorage by lazy {
        TokenStorage(context)
    }

    override val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(tokenStorage)
    }

    override val socketManager: SocketManager by lazy {
        SocketManager(ApiConfig.BASE_URL)
    }

    override val apiService: ApiService by lazy {
        RetrofitClient.createApiService(sessionRepository)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, sessionRepository)
    }

    override val apiRepository: ApiRepository by lazy {
        ApiRepositoryImpl(apiService)
    }

    override val customerRepository: CustomerRepository by lazy {
        CustomerRepositoryImpl(apiService)
    }

    override val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(apiService)
    }

    override val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(apiService)
    }

    override val financialRepository: FinancialRepository by lazy {
        FinancialRepositoryImpl(apiService)
    }

    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(
            apiService = apiService,
            socketManager = socketManager,
            sessionRepository = sessionRepository,
            context = context
        ).apply {
            startRealTimeSync()
        }
    }
}
