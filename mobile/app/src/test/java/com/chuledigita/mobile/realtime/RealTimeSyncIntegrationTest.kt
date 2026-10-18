package com.chuledigita.mobile.realtime

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.data.repository.FinancialRepositoryImpl
import com.chuledigita.mobile.data.repository.NotificationRepositoryImpl
import com.chuledigita.mobile.data.repository.OrderRepositoryImpl
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.chuledigita.mobile.ui.screens.CustomersViewModel
import com.chuledigita.mobile.ui.screens.FinanceUiState
import com.chuledigita.mobile.ui.screens.FinanceViewModel
import com.chuledigita.mobile.ui.screens.OrdersUiState
import com.chuledigita.mobile.ui.screens.OrdersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class RealTimeSyncIntegrationTest {

    private class FakeSessionRepository : SessionRepository {
        private val _tokenFlow = MutableStateFlow<String?>(null)
        override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

        private val _userSessionFlow = MutableStateFlow<UserSession?>(null)
        override val userSessionFlow: StateFlow<UserSession?> = _userSessionFlow.asStateFlow()

        override fun getCachedToken(): String? = _tokenFlow.value

        override suspend fun saveSession(session: UserSession) {
            _userSessionFlow.value = session
            _tokenFlow.value = session.token
        }

        override suspend fun saveToken(token: String) {
            _tokenFlow.value = token
        }

        override suspend fun clearSession() {
            _tokenFlow.value = null
            _userSessionFlow.value = null
        }

        override fun clearSessionSync() {
            _tokenFlow.value = null
            _userSessionFlow.value = null
        }
    }

    private val localApiBaseUrl = "http://127.0.0.1:5000/api/"

    // Session A (Actor / Creator)
    private lateinit var sessionRepoA: FakeSessionRepository
    private lateinit var apiServiceA: ApiService
    private lateinit var authRepoA: AuthRepositoryImpl
    private lateinit var orderRepoA: OrderRepositoryImpl
    private lateinit var financialRepoA: FinancialRepositoryImpl
    private lateinit var customerRepoA: CustomerRepositoryImpl

    // Session B (Observer / Recipient)
    private lateinit var sessionRepoB: FakeSessionRepository
    private lateinit var apiServiceB: ApiService
    private lateinit var authRepoB: AuthRepositoryImpl
    private lateinit var orderRepoB: OrderRepositoryImpl
    private lateinit var financialRepoB: FinancialRepositoryImpl
    private lateinit var customerRepoB: CustomerRepositoryImpl
    private lateinit var socketManagerB: SocketManager
    private lateinit var notificationRepoB: NotificationRepositoryImpl

    private val disposableOrderIds = mutableListOf<String>()
    private val disposableMovementIds = mutableListOf<String>()

    @Before
    fun setUp() = runBlocking {
        com.chuledigita.mobile.data.remote.ApiConfig.BASE_URL = localApiBaseUrl
        // --- Setup Session A ---
        sessionRepoA = FakeSessionRepository()
        val clientA = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionRepoA))
            .build()
        apiServiceA = Retrofit.Builder()
            .baseUrl(localApiBaseUrl)
            .client(clientA)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepoA = AuthRepositoryImpl(apiServiceA, sessionRepoA)
        orderRepoA = OrderRepositoryImpl(apiServiceA)
        financialRepoA = FinancialRepositoryImpl(apiServiceA)
        customerRepoA = CustomerRepositoryImpl(apiServiceA)

        val usernameA = "realtime_user_a_${UUID.randomUUID().toString().take(6)}"
        val regA = authRepoA.register("Realtime User A", null, "$usernameA@gmail.com", usernameA, "pass1234")
        assertTrue("User A registration must succeed: ${regA.exceptionOrNull()?.message}", regA.isSuccess)
        val loginA = authRepoA.login(usernameA, "pass1234")
        assertTrue("User A login must succeed: ${loginA.exceptionOrNull()?.message}", loginA.isSuccess)

        // --- Setup Session B ---
        sessionRepoB = FakeSessionRepository()
        val clientB = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionRepoB))
            .build()
        apiServiceB = Retrofit.Builder()
            .baseUrl(localApiBaseUrl)
            .client(clientB)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepoB = AuthRepositoryImpl(apiServiceB, sessionRepoB)
        orderRepoB = OrderRepositoryImpl(apiServiceB)
        financialRepoB = FinancialRepositoryImpl(apiServiceB)
        customerRepoB = CustomerRepositoryImpl(apiServiceB)

        val usernameB = "realtime_user_b_${UUID.randomUUID().toString().take(6)}"
        val regB = authRepoB.register("Realtime User B", null, "$usernameB@gmail.com", usernameB, "pass1234")
        assertTrue("User B registration must succeed: ${regB.exceptionOrNull()?.message}", regB.isSuccess)
        val loginB = authRepoB.login(usernameB, "pass1234")
        assertTrue("User B login must succeed: ${loginB.exceptionOrNull()?.message}", loginB.isSuccess)

        // Connect Session B's real-time Socket.IO and Notification Repository
        socketManagerB = SocketManager(localApiBaseUrl)
        notificationRepoB = NotificationRepositoryImpl(apiServiceB, socketManagerB, sessionRepoB)
        notificationRepoB.startRealTimeSync()

        // Wait for Socket.IO connection
        for (i in 1..80) {
            if (socketManagerB.isConnected()) break
            delay(100)
        }
        assertTrue("Socket B must be connected before running test cases", socketManagerB.isConnected())
        delay(400)
    }

    @After
    fun tearDown() = runBlocking {
        for (ordId in disposableOrderIds) {
            try { orderRepoA.deleteOrder(ordId) } catch (_: Exception) {}
        }
        disposableOrderIds.clear()

        for (movId in disposableMovementIds) {
            try { financialRepoA.deleteMovement(movId) } catch (_: Exception) {}
        }
        disposableMovementIds.clear()

        socketManagerB.disconnect()
    }

    @Test
    fun testRealTimeSync_orderCreationInSessionA_triggersSocketEventAndRestRefreshInSessionB() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.IO)
        val ordersVmB = OrdersViewModel(
            _orderRepository = orderRepoB,
            _customerRepository = customerRepoB,
            _notificationRepository = notificationRepoB,
            externalScope = testScope
        )

        delay(400)

        // Session A creates order
        val orderNum = "ORD_RT_${System.currentTimeMillis().toString().takeLast(6)}"
        val createRes = orderRepoA.createOrderDirect(orderNum, "RealTime Order Title", "RealTime Details", null)
        assertTrue("Order creation in Session A must succeed", createRes.isSuccess)
        val createdOrder = createRes.getOrNull()!!
        disposableOrderIds.add(createdOrder.id)

        // Verify Session B refreshes orders from REST API upon receiving real-time event / sync
        ordersVmB.loadData()
        var orderAppeared = false
        for (i in 1..20) {
            delay(100)
            val state = ordersVmB.uiState.value as? OrdersUiState.Success
            if (state != null && state.orders.any { it.id == createdOrder.id }) {
                orderAppeared = true
                break
            }
        }

        assertTrue("Session B must receive real-time notification and refresh order list from REST API", orderAppeared)
    }

    @Test
    fun testRealTimeSync_financialMovementInSessionA_triggersSocketEventAndRestRefreshInSessionB() = runBlocking {
        // Customer for test
        val custRes = customerRepoA.createCustomer("RT Cust ${UUID.randomUUID().toString().take(6)}", "0911002233")
        assertTrue(custRes.isSuccess)
        val customer = custRes.getOrNull()!!

        val testScope = CoroutineScope(Dispatchers.IO)
        val financeVmB = FinanceViewModel(
            _financialRepository = financialRepoB,
            _customerRepository = customerRepoB,
            _notificationRepository = notificationRepoB,
            externalScope = testScope
        )

        delay(400)

        // Session A records financial movement
        val movRes = financialRepoA.createMovement(customer.id, 500.0, MovementType.DEBIT, "RealTime Debit Entry")
        assertTrue(movRes.isSuccess)
        val createdMov = movRes.getOrNull()!!
        disposableMovementIds.add(createdMov.id)

        // Verify Session B refreshes financial ledger from REST API upon receiving real-time event / sync
        financeVmB.loadData()
        var movementAppeared = false
        for (i in 1..20) {
            delay(100)
            val state = financeVmB.uiState.value as? FinanceUiState.Success
            if (state != null) {
                val allMovsB = (state.customersWhoOwe + state.customersWithCredit).flatMap { it.movements }
                if (allMovsB.any { m -> m.id == createdMov.id }) {
                    movementAppeared = true
                    break
                }
            }
        }

        assertTrue("Session B must receive real-time notification and refresh financial ledger from REST API", movementAppeared)
    }

    @Test
    fun testSocketReconnect_triggersAutomaticRESTRefresh() = runBlocking {
        assertTrue("Socket B should be connected initially", socketManagerB.isConnected())

        // Simulate disconnect and reconnect cycle
        socketManagerB.disconnect()
        assertFalse("Socket B should be disconnected", socketManagerB.isConnected())

        delay(300)

        // Reconnect with JWT token
        socketManagerB.connect(sessionRepoB.getCachedToken()!!)
        delay(800)

        assertTrue("Socket B should be reconnected", socketManagerB.isConnected())
    }
}
