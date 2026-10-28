package com.chuledigita.mobile.order

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.data.repository.OrderRepositoryImpl
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.chuledigita.mobile.ui.screens.CreateOrderViewModel
import com.chuledigita.mobile.ui.screens.OrderDetailsUiState
import com.chuledigita.mobile.ui.screens.OrderDetailsViewModel
import com.chuledigita.mobile.ui.screens.OrdersUiState
import com.chuledigita.mobile.ui.screens.OrdersViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OrderIntegrationTest {

    private class FakeSessionRepository : SessionRepository {
        var token: String? = null
        var session: UserSession? = null

        override val tokenFlow: Flow<String?> get() = flowOf(token)
        override val userSessionFlow: Flow<UserSession?> get() = flowOf(session)

        override fun getCachedToken(): String? = token

        override suspend fun saveSession(session: UserSession) {
            this.session = session
            this.token = session.token
        }

        override suspend fun saveToken(token: String) {
            this.token = token
        }

        override suspend fun clearSession() {
            this.token = null
            this.session = null
        }

        override fun clearSessionSync() {
            this.token = null
            this.session = null
        }
    }

    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var apiService: ApiService
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var customerRepository: CustomerRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    private val localApiBaseUrl = "http://127.0.0.1:5000/api/"
    private val disposableOrderIds = mutableListOf<String>()

    @Before
    fun setUp() = runBlocking {
        com.chuledigita.mobile.data.remote.ApiConfig.BASE_URL = localApiBaseUrl
        fakeSessionRepository = FakeSessionRepository()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(fakeSessionRepository))
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(localApiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        authRepository = AuthRepositoryImpl(apiService, fakeSessionRepository)
        customerRepository = CustomerRepositoryImpl(apiService)
        orderRepository = OrderRepositoryImpl(apiService)

        // Authenticate with a test account so all subsequent calls carry the real JWT
        val username = "order_test_user_${java.util.UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        val reg = authRepository.register("Order Test User", null, "$username@gmail.com", username, "orderPass123")
        if (!reg.isSuccess) {
            println("DEBUG REG ERROR: " + reg.exceptionOrNull()?.message)
        }
        assertTrue("Test registration must succeed: ${reg.exceptionOrNull()?.message}", reg.isSuccess)
        val login = authRepository.login(username, "orderPass123")
        assertTrue("Test login must succeed: ${login.exceptionOrNull()?.message}", login.isSuccess)
        assertNotNull("Session token must be present", fakeSessionRepository.getCachedToken())
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up disposable test fixtures
        for (orderId in disposableOrderIds) {
            try {
                orderRepository.deleteOrder(orderId)
            } catch (_: Exception) {}
        }
        disposableOrderIds.clear()
    }

    @Test
    fun testGetAllOrders_returnsRealOrders() = runBlocking {
        val result = orderRepository.getOrders()
        assertTrue("Fetching all orders must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val orders = result.getOrNull()
        assertNotNull("Orders list must not be null", orders)
        // Verify orders have non-null mapped fields even if backend omitted order_status in SELECT
        for (order in orders!!) {
            assertNotNull("Order id must exist", order.id)
            assertNotNull("Order status must default cleanly", order.status)
            assertNotNull("Verification status must default cleanly", order.verificationStatus)
        }
    }

    @Test
    fun testCreateOrder_persistsInBackend_and_appearsInList() = runBlocking {
        val uniqueOrderNumber = "ORD_DISP_${System.currentTimeMillis().toString().takeLast(6)}"
        val title = "Disposable Stationery Order"
        val description = "30 notebooks and 10 pens for office supplies"

        val createResult = orderRepository.createOrderDirect(
            orderNumber = uniqueOrderNumber,
            title = title,
            description = description,
            customerId = null
        )
        assertTrue("Creating order must succeed: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)
        val created = createResult.getOrNull()
        assertNotNull("Created order must not be null", created)
        disposableOrderIds.add(created!!.id)

        assertEquals(uniqueOrderNumber, created.orderNumber)
        assertEquals(title, created.title)
        assertEquals(description, created.description)
        assertTrue("Order ID must be positive integer string", created.id.toLong() > 0)

        // Verify order retrieval by ID
        val getResult = orderRepository.getOrderById(created.id)
        assertTrue("Fetching created order by ID must succeed", getResult.isSuccess)
        val fetched = getResult.getOrNull()
        assertNotNull(fetched)
        assertEquals(created.id, fetched!!.id)
        assertEquals(uniqueOrderNumber, fetched.orderNumber)
        assertEquals(title, fetched.title)

        // Verify newly created order appears in all orders list
        val allResult = orderRepository.getOrders()
        assertTrue(allResult.isSuccess)
        val list = allResult.getOrNull() ?: emptyList()
        assertTrue("Created order must appear in all orders list", list.any { it.id == created.id })
    }

    @Test
    fun testGetOrderById_nonExistent_returns404Error() = runBlocking {
        val result = orderRepository.getOrderById("999999999")
        assertTrue("Fetching non-existent order must fail", result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error message should mention Order not found: $message",
            message.contains("Order not found", ignoreCase = true)
        )
    }

    @Test
    fun testCreateOrder_serverSideValidationErrors() = runBlocking {
        val orderNumber = "ORD_VAL_${System.currentTimeMillis().toString().takeLast(6)}"

        // 1. Title exceeding 150 characters
        val longTitle = "A".repeat(151)
        val longTitleResult = orderRepository.createOrderDirect(
            orderNumber = orderNumber,
            title = longTitle,
            description = "Valid description"
        )
        assertTrue("Creating order with >150 char title must fail", longTitleResult.isFailure)
        val longTitleError = longTitleResult.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error must mention title limit: $longTitleError",
            longTitleError.contains("title", ignoreCase = true)
        )

        // 2. Empty description
        val emptyDescResult = orderRepository.createOrderDirect(
            orderNumber = orderNumber,
            title = "Valid Title",
            description = "   "
        )
        assertTrue("Creating order with empty description must fail", emptyDescResult.isFailure)
        val emptyDescError = emptyDescResult.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error must mention description required: $emptyDescError",
            emptyDescError.contains("details are required", ignoreCase = true) ||
            emptyDescError.contains("description", ignoreCase = true)
        )
    }

    @Test
    fun testUpdateOrder_updatesTitleAndDescription() = runBlocking {
        val orderNumber = "ORD_UPD_${System.currentTimeMillis().toString().takeLast(6)}"
        val createResult = orderRepository.createOrderDirect(
            orderNumber = orderNumber,
            title = "Original Title",
            description = "Original Description"
        )
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!
        disposableOrderIds.add(created.id)

        val updatedTitle = "Modified Title"
        val updatedDescription = "Modified Description Content"
        val updateResult = orderRepository.updateOrder(
            orderId = created.id,
            title = updatedTitle,
            description = updatedDescription
        )
        assertTrue("Updating order must succeed: ${updateResult.exceptionOrNull()?.message}", updateResult.isSuccess)
        val updated = updateResult.getOrNull()
        assertNotNull(updated)
        assertEquals(updatedTitle, updated!!.title)
        assertEquals(updatedDescription, updated.description)
    }

    @Test
    fun testUpdateOrderStatus_toCompleted_and_completedOrderCannotBeChanged() = runBlocking {
        val orderNumber = "ORD_STATUS_${System.currentTimeMillis().toString().takeLast(6)}"
        val createResult = orderRepository.createOrderDirect(
            orderNumber = orderNumber,
            title = "Status Test Order",
            description = "Testing completion status transition"
        )
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!
        disposableOrderIds.add(created.id)

        // 1. Transition to COMPLETED
        val completeResult = orderRepository.updateOrderStatus(created.id, OrderStatus.COMPLETED)
        assertTrue("Transitioning to COMPLETED must succeed: ${completeResult.exceptionOrNull()?.message}", completeResult.isSuccess)
        val completedOrder = completeResult.getOrNull()
        assertNotNull(completedOrder)
        assertEquals(OrderStatus.COMPLETED, completedOrder!!.status)

        // 2. Server-side rule: Completed orders cannot be changed
        val revertResult = orderRepository.updateOrderStatus(created.id, OrderStatus.PENDING)
        assertTrue("Reverting completed order must fail per backend contract", revertResult.isFailure)
        val revertError = revertResult.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error should state completed orders cannot be changed: $revertError",
            revertError.contains("Completed orders cannot be changed", ignoreCase = true)
        )
    }

    @Test
    fun testVerifyOrder_and_RejectOrder_contracts() = runBlocking {
        // 1. Verify an order
        val verifyOrderNum = "ORD_VRF_${System.currentTimeMillis().toString().takeLast(6)}"
        val createVrf = orderRepository.createOrderDirect(
            orderNumber = verifyOrderNum,
            title = "To Verify",
            description = "Order to verify"
        )
        assertTrue(createVrf.isSuccess)
        val vrfOrder = createVrf.getOrNull()!!
        disposableOrderIds.add(vrfOrder.id)

        val verifyResult = orderRepository.verifyOrder(vrfOrder.id, "Verified by test")
        assertTrue("Verifying order must succeed: ${verifyResult.exceptionOrNull()?.message}", verifyResult.isSuccess)
        val verified = verifyResult.getOrNull()
        assertNotNull(verified)
        assertEquals(VerificationStatus.VERIFIED, verified!!.verificationStatus)
        assertTrue(verified.isVerified)

        // 2. Reject an order with mandatory note
        val rejectOrderNum = "ORD_REJ_${System.currentTimeMillis().toString().takeLast(6)}"
        val createRej = orderRepository.createOrderDirect(
            orderNumber = rejectOrderNum,
            title = "To Reject",
            description = "Order to reject"
        )
        assertTrue(createRej.isSuccess)
        val rejOrder = createRej.getOrNull()!!
        disposableOrderIds.add(rejOrder.id)

        val rejectResult = orderRepository.rejectOrder(rejOrder.id, "Items unavailable")
        assertTrue("Rejecting order must succeed: ${rejectResult.exceptionOrNull()?.message}", rejectResult.isSuccess)
        val rejected = rejectResult.getOrNull()
        assertNotNull(rejected)
        assertEquals(VerificationStatus.REJECTED, rejected!!.verificationStatus)

        // 3. Rejecting without note fails validation
        val rejectWithoutNote = orderRepository.rejectOrder(rejOrder.id, "   ")
        assertTrue("Rejecting without note must fail", rejectWithoutNote.isFailure)
    }

    @Test
    fun testDeleteOrder_removesOrder() = runBlocking {
        val orderNumber = "ORD_DEL_${System.currentTimeMillis().toString().takeLast(6)}"
        val createResult = orderRepository.createOrderDirect(
            orderNumber = orderNumber,
            title = "To Delete",
            description = "Order to be deleted"
        )
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!

        val deleteResult = orderRepository.deleteOrder(created.id)
        assertTrue("Delete order must succeed: ${deleteResult.exceptionOrNull()?.message}", deleteResult.isSuccess)

        // Confirm it no longer exists
        val getResult = orderRepository.getOrderById(created.id)
        assertTrue("Order lookup should fail after deletion", getResult.isFailure)
    }

    @Test
    fun testUnauthorizedRequest_withoutToken_returns401() = runBlocking {
        val savedToken = fakeSessionRepository.getCachedToken()
        try {
            fakeSessionRepository.clearSession()
            val response = apiService.getAllOrders()
            assertEquals("Request without token must return 401 Unauthorized", 401, response.code())
        } finally {
            if (savedToken != null) {
                fakeSessionRepository.saveToken(savedToken)
            }
        }
    }

    @Test
    fun testOrdersViewModel_lifecycle_and_searchFiltering() = runBlocking {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val viewModel = OrdersViewModel(
            _orderRepository = orderRepository,
            _customerRepository = customerRepository,
            externalScope = testScope
        )

        // Wait for loadData
        kotlinx.coroutines.delay(600)
        val state = viewModel.uiState.value
        assertTrue("ViewModel uiState must be Success", state is OrdersUiState.Success)
        val successState = state as OrdersUiState.Success
        assertNotNull("Orders list must be populated", successState.orders)

        // Search test
        viewModel.onSearchQueryChanged("non_existent_query_xyz_123")
        val searchedState = viewModel.uiState.value as OrdersUiState.Success
        assertEquals("Search query must be updated", "non_existent_query_xyz_123", searchedState.searchQuery)
        assertTrue("Filtered orders must be empty for non-existent query", searchedState.orders.isEmpty())

        // Clear search
        viewModel.onSearchQueryChanged("")
        val clearedSearchState = viewModel.uiState.value as OrdersUiState.Success
        assertEquals(successState.orders.size, clearedSearchState.orders.size)

        // Validation test on createOrder
        viewModel.createOrder("", "")
        val validationState = viewModel.uiState.value as OrdersUiState.Success
        assertNotNull("Error message must be present for empty fields", validationState.errorMessage)

        viewModel.clearMessage()
        val clearedMsgState = viewModel.uiState.value as OrdersUiState.Success
        assertNull("Error message must be cleared", clearedMsgState.errorMessage)
    }

    @Test
    fun testOrderDetailsViewModel_load_and_statusUpdate() = runBlocking {
        // Create disposable order
        val orderNum = "ORD_VM_${System.currentTimeMillis().toString().takeLast(6)}"
        val createResult = orderRepository.createOrderDirect(
            orderNumber = orderNum,
            title = "VM Details Test",
            description = "Details for ViewModel test"
        )
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!
        disposableOrderIds.add(created.id)

        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = OrderDetailsViewModel(
            _orderRepository = orderRepository,
            externalScope = testScope
        )
        vm.loadOrder(created.id)

        var state = vm.uiState.value
        for (i in 1..40) {
            if (state is OrderDetailsUiState.Success) break
            kotlinx.coroutines.delay(100)
            state = vm.uiState.value
        }
        assertTrue("OrderDetailsViewModel state must be Success. Current: $state", state is OrderDetailsUiState.Success)
        val successState = state as OrderDetailsUiState.Success
        assertEquals(created.id, successState.order.id)
        assertEquals(orderNum, successState.order.orderNumber)

        // Complete order
        vm.updateStatus(created.id, OrderStatus.COMPLETED)
        var updatedState: OrderDetailsUiState.Success? = null
        for (i in 1..50) {
            kotlinx.coroutines.delay(100)
            val st = vm.uiState.value
            if (st is OrderDetailsUiState.Success && !st.isSubmitting && st.successMessage != null) {
                updatedState = st
                break
            }
        }
        assertNotNull("Updated state must be Success and not submitting. Current: ${vm.uiState.value}", updatedState)
        assertEquals(OrderStatus.COMPLETED, updatedState!!.order.status)
        assertNotNull("Success message must be present", updatedState.successMessage)

        vm.clearMessage()
        val clearedState = vm.uiState.value as OrderDetailsUiState.Success
        assertNull(clearedState.successMessage)
    }

    @Test
    fun testCreateOrderViewModel_validation_and_submission() = runBlocking {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = CreateOrderViewModel(
            orderRepository = orderRepository,
            customerRepository = customerRepository,
            externalScope = testScope
        )

        kotlinx.coroutines.delay(500)
        val customersState = vm.uiState.value
        assertNotNull("Customer list should be initialized", customersState.customers)

        // 1. Validation test on blank title
        vm.submitOrder(title = "", description = "Valid description", selectedCustomer = null) {}
        assertNotNull("Error message must be present for blank title", vm.uiState.value.errorMessage)

        // 2. Clear error
        vm.clearError()
        assertNull("Error message must be cleared", vm.uiState.value.errorMessage)

        // 3. Successful submission
        var successCallbackCalled = false
        val testTitle = "VM Created Order Title"
        val testDesc = "VM Created Order Details"
        vm.submitOrder(title = testTitle, description = testDesc, selectedCustomer = null) {
            successCallbackCalled = true
        }

        for (i in 1..50) {
            kotlinx.coroutines.delay(100)
            if (vm.uiState.value.isSuccess) break
        }
        assertTrue("Success callback must be called on valid submission. Error: ${vm.uiState.value.errorMessage}", successCallbackCalled)
        assertTrue("isSuccess must be true in state", vm.uiState.value.isSuccess)
    }
}
