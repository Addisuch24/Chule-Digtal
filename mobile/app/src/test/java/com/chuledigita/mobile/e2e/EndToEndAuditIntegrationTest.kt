package com.chuledigita.mobile.e2e

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.data.repository.FinancialRepositoryImpl
import com.chuledigita.mobile.data.repository.NotificationRepositoryImpl
import com.chuledigita.mobile.data.repository.OrderRepositoryImpl
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.SessionRepository
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
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class EndToEndAuditIntegrationTest {

    private class TestSessionRepository : SessionRepository {
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

    private val apiBaseUrl = "http://127.0.0.1:5000/api/"

    // Session A
    private lateinit var sessionRepoA: TestSessionRepository
    private lateinit var apiServiceA: ApiService
    private lateinit var authRepoA: AuthRepositoryImpl
    private lateinit var customerRepoA: CustomerRepositoryImpl
    private lateinit var orderRepoA: OrderRepositoryImpl
    private lateinit var financialRepoA: FinancialRepositoryImpl

    // Session B
    private lateinit var sessionRepoB: TestSessionRepository
    private lateinit var apiServiceB: ApiService
    private lateinit var authRepoB: AuthRepositoryImpl
    private lateinit var customerRepoB: CustomerRepositoryImpl
    private lateinit var orderRepoB: OrderRepositoryImpl
    private lateinit var financialRepoB: FinancialRepositoryImpl
    private lateinit var socketManagerB: SocketManager
    private lateinit var notificationRepoB: NotificationRepositoryImpl

    private val disposableCustomers = mutableListOf<String>()
    private val disposableOrders = mutableListOf<String>()
    private val disposableMovements = mutableListOf<String>()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        com.chuledigita.mobile.data.remote.ApiConfig.BASE_URL = apiBaseUrl
        // --- Initialize Session A ---
        sessionRepoA = TestSessionRepository()
        val clientA = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionRepoA))
            .build()
        apiServiceA = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(clientA)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepoA = AuthRepositoryImpl(apiServiceA, sessionRepoA)
        customerRepoA = CustomerRepositoryImpl(apiServiceA)
        orderRepoA = OrderRepositoryImpl(apiServiceA)
        financialRepoA = FinancialRepositoryImpl(apiServiceA)

        val userA = "audit_user_a_${UUID.randomUUID().toString().take(6)}"
        val regA = authRepoA.register("Audit User A", "0911998877", "$userA@gmail.com", userA, "pass1234")
        assertTrue("Session A registration must succeed", regA.isSuccess)
        val loginA = authRepoA.login(userA, "pass1234")
        assertTrue("Session A login must succeed", loginA.isSuccess)

        // --- Initialize Session B ---
        sessionRepoB = TestSessionRepository()
        val clientB = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionRepoB))
            .build()
        apiServiceB = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(clientB)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepoB = AuthRepositoryImpl(apiServiceB, sessionRepoB)
        customerRepoB = CustomerRepositoryImpl(apiServiceB)
        orderRepoB = OrderRepositoryImpl(apiServiceB)
        financialRepoB = FinancialRepositoryImpl(apiServiceB)

        val userB = "audit_user_b_${UUID.randomUUID().toString().take(6)}"
        val regB = authRepoB.register("Audit User B", "0922887766", "$userB@gmail.com", userB, "pass1234")
        assertTrue("Session B registration must succeed", regB.isSuccess)
        val loginB = authRepoB.login(userB, "pass1234")
        assertTrue("Session B login must succeed", loginB.isSuccess)

        socketManagerB = SocketManager(apiBaseUrl)
        notificationRepoB = NotificationRepositoryImpl(apiServiceB, socketManagerB, sessionRepoB)
        notificationRepoB.startRealTimeSync()

        for (i in 1..80) {
            if (socketManagerB.isConnected()) break
            delay(100)
        }
        assertTrue("Socket B must be connected", socketManagerB.isConnected())
        delay(400)
    }

    @After
    fun tearDown() = runBlocking {
        for (movId in disposableMovements) {
            try { financialRepoA.deleteMovement(movId) } catch (_: Exception) {}
        }
        disposableMovements.clear()

        for (ordId in disposableOrders) {
            try { orderRepoA.deleteOrder(ordId) } catch (_: Exception) {}
        }
        disposableOrders.clear()

        socketManagerB.disconnect()
        Dispatchers.resetMain()
    }

    @Test
    fun testWorkflowA_Authentication_securityAndSessionLifecycle() = runBlocking {
        // 1. Invalid login fails
        val failLogin = authRepoA.login("non_existent_user_xyz", "wrongpass")
        assertTrue("Invalid credentials must return failure", failLogin.isFailure)

        // 2. Unauthenticated request without token fails HTTP 401
        val unauthClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build()
        val unauthApi = Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(unauthClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val unauthRes = unauthApi.getAllCustomers()
        assertEquals("Unauthenticated request must return 401", 401, unauthRes.code())

        // 3. Logout clears session
        authRepoA.logout()
        assertNull("Logout must clear cached session", sessionRepoA.getCachedToken())
    }

    @Test
    fun testWorkflowB_CustomersAndNotes_persistenceAndAPIContracts() = runBlocking {
        // 1. Create disposable customer
        val custName = "Audit Cust ${UUID.randomUUID().toString().take(6)}"
        val createRes = customerRepoA.createCustomer(custName, "0912345678")
        assertTrue("Customer creation must succeed", createRes.isSuccess)
        val customer = createRes.getOrNull()!!
        disposableCustomers.add(customer.id)

        // 2. Fetch customers and verify
        val listRes = customerRepoA.getCustomers()
        assertTrue(listRes.isSuccess)
        assertTrue("Created customer must appear in listing", listRes.getOrNull()!!.any { it.id == customer.id })

        // 3. Create customer note
        val noteRes = customerRepoA.createCustomerNote(customer.id, "Audit Note Text 101")
        assertTrue("Note creation must succeed", noteRes.isSuccess)

        // 4. Retrieve customer notes
        val notesRes = customerRepoA.getCustomerNotes(customer.id)
        assertTrue(notesRes.isSuccess)
        assertTrue("Note list must contain created note", notesRes.getOrNull()!!.any { it.note == "Audit Note Text 101" })
    }

    @Test
    fun testWorkflowC_Orders_statusLifecycleAndVerification() = runBlocking {
        // 1. Create order
        val orderNum = "ORD-AUDIT-${System.currentTimeMillis().toString().takeLast(6)}"
        val createRes = orderRepoA.createOrderDirect(orderNum, "Audit Order Title", "Audit Order Details", null)
        assertTrue("Order creation must succeed", createRes.isSuccess)
        val order = createRes.getOrNull()!!
        disposableOrders.add(order.id)

        // 2. Update status PENDING -> PROCESSING
        val statusRes = orderRepoA.updateOrderStatus(order.id, OrderStatus.PROCESSING)
        assertTrue("Status update to PROCESSING must succeed", statusRes.isSuccess)

        // 3. Verify order with note
        val verifyRes = orderRepoA.verifyOrder(order.id, "Audit verified ok")
        assertTrue("Order verification must succeed", verifyRes.isSuccess)
        assertEquals(VerificationStatus.VERIFIED, verifyRes.getOrNull()!!.verificationStatus)
    }

    @Test
    fun testWorkflowD_FinancialMovements_debitCreditBalanceAccounting() = runBlocking {
        // Customer for financial tests
        val randomSuffix = UUID.randomUUID().toString().filter { it.isDigit() }.take(6).padEnd(6, '1')
        val custRes = customerRepoA.createCustomer("Financial Audit Cust $randomSuffix", "09$randomSuffix")
        assertTrue(custRes.isSuccess)
        val customer = custRes.getOrNull()!!
        disposableCustomers.add(customer.id)

        // 1. Record DEBIT movement (debt: 1000 ETB)
        val debitRes = financialRepoA.createMovement(customer.id, 1000.0, MovementType.DEBIT, "Audit Debit Entry")
        assertTrue(debitRes.isSuccess)
        val debitMov = debitRes.getOrNull()!!
        disposableMovements.add(debitMov.id)

        // 2. Record CREDIT movement (payment: 300 ETB)
        val creditRes = financialRepoA.createMovement(customer.id, 300.0, MovementType.CREDIT, "Audit Credit Entry")
        assertTrue(creditRes.isSuccess)
        val creditMov = creditRes.getOrNull()!!
        disposableMovements.add(creditMov.id)

        // 3. Verify calculated balance: balance = total_credit - total_debit = 300 - 1000 = -700.0
        val balanceRes = apiServiceA.getCustomerBalance(customer.id.toLong())
        assertTrue(balanceRes.isSuccessful && balanceRes.body()?.success == true)
        val balData = balanceRes.body()?.data!!
        assertEquals(1000.0, balData.totalDebit, 0.01)
        assertEquals(300.0, balData.totalCredit, 0.01)
        assertEquals(-700.0, balData.balance, 0.01)

        // 4. Verify movement requiring note
        val verifyRes = financialRepoA.verifyMovement(debitMov.id, "Audit Movement Verified")
        assertTrue("Movement verification must succeed", verifyRes.isSuccess)
        assertEquals(VerificationStatus.VERIFIED, verifyRes.getOrNull()!!.verificationStatus)
    }

    @Test
    fun testWorkflowE_RealTimeSync_crossSessionBroadcasting() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.IO)
        val ordersVmB = OrdersViewModel(
            _orderRepository = orderRepoB,
            _customerRepository = customerRepoB,
            _notificationRepository = notificationRepoB,
            externalScope = testScope
        )

        val financeVmB = FinanceViewModel(
            financialRepository = financialRepoB,
            customerRepository = customerRepoB
        )

        delay(400)

        // Session A creates order
        val orderNum = "ORD-RT-AUDIT-${System.currentTimeMillis().toString().takeLast(6)}"
        val createRes = orderRepoA.createOrderDirect(orderNum, "E2E RealTime Order", "E2E Details", null)
        assertTrue(createRes.isSuccess)
        val createdOrder = createRes.getOrNull()!!
        disposableOrders.add(createdOrder.id)

        ordersVmB.loadData()
        var orderAppeared = false
        for (i in 1..20) {
            delay(100)
            val st = ordersVmB.uiState.value as? OrdersUiState.Success
            if (st != null && st.orders.any { it.id == createdOrder.id }) {
                orderAppeared = true
                break
            }
        }
        assertTrue("Session B must receive real-time notification and refresh order list", orderAppeared)

        // Test disconnect and reconnect
        socketManagerB.disconnect()
        assertFalse("Socket B should be disconnected", socketManagerB.isConnected())
        delay(300)
        socketManagerB.connect(sessionRepoB.getCachedToken()!!)
        delay(800)
        assertTrue("Socket B should reconnect successfully", socketManagerB.isConnected())
    }
}
