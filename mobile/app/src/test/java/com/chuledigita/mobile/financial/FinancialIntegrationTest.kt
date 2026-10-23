package com.chuledigita.mobile.financial

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.data.repository.FinancialRepositoryImpl
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.SessionRepository

import com.chuledigita.mobile.ui.screens.FinanceUiState
import com.chuledigita.mobile.ui.screens.FinanceViewModel
import com.chuledigita.mobile.ui.screens.FinancialMovementDetailsUiState
import com.chuledigita.mobile.ui.screens.FinancialMovementDetailsViewModel
import com.chuledigita.mobile.ui.screens.FinancialMovementsUiState
import com.chuledigita.mobile.ui.screens.FinancialMovementsViewModel
import com.chuledigita.mobile.ui.screens.RecordFinancialMovementViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

class FinancialIntegrationTest {

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
    private lateinit var financialRepository: FinancialRepositoryImpl

    private val localApiBaseUrl = "http://127.0.0.1:5000/api/"
    private val disposableMovementIds = mutableListOf<String>()

    private var testCustomerId: String = ""

    @Before
    fun setUp() = runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.Dispatchers.Unconfined)
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
        financialRepository = FinancialRepositoryImpl(apiService)

        // Authenticate with a test account so all subsequent calls carry the real JWT
        val username = "fin_test_user_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        val reg = authRepository.register("Fin Test User", null, "$username@gmail.com", username, "finPass123")
        assertTrue("Test registration must succeed: ${reg.exceptionOrNull()?.message}", reg.isSuccess)
        val login = authRepository.login(username, "finPass123")
        assertTrue("Test login must succeed: ${login.exceptionOrNull()?.message}", login.isSuccess)
        assertNotNull("Session token must be present", fakeSessionRepository.getCachedToken())

        // Provision a disposable test customer for financial movements
        val custResult = customerRepository.createCustomer(
            name = "Fin Test Customer ${UUID.randomUUID().toString().take(6)}",
            phone = "09${(10000000..99999999).random()}"
        )
        assertTrue("Customer provisioning must succeed", custResult.isSuccess)
        val customer = custResult.getOrNull()!!
        testCustomerId = customer.id
    }

    @After
    fun tearDown() = runBlocking {
        // Clean up disposable test movements
        for (movId in disposableMovementIds) {
            try {
                financialRepository.deleteMovement(movId)
            } catch (_: Exception) {}
        }
        disposableMovementIds.clear()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun testGetAllMovements_returnsRealMovements() = runBlocking {
        val result = financialRepository.getAllMovements()
        assertTrue("Fetching all movements must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val movements = result.getOrNull()
        assertNotNull("Movements list must not be null", movements)
        for (mov in movements!!) {
            assertNotNull("Movement ID must exist", mov.id)
            assertNotNull("Movement type must exist", mov.type)
            assertNotNull("Verification status must exist", mov.verificationStatus)
        }
    }

    @Test
    fun testCreateFinancialMovement_persistsInBackend_and_appearsInList() = runBlocking {
        val amount = 1500.0
        val description = "Bulkcement debit entry test"

        val createResult = financialRepository.createMovement(
            customerId = testCustomerId,
            amount = amount,
            type = MovementType.DEBIT,
            description = description
        )
        assertTrue("Creating movement must succeed: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)
        val created = createResult.getOrNull()
        assertNotNull("Created movement must not be null", created)
        disposableMovementIds.add(created!!.id)

        assertEquals(testCustomerId, created.customerId)
        assertEquals(amount, created.amount, 0.01)
        assertEquals(MovementType.DEBIT, created.type)
        assertEquals(description, created.description)
        assertEquals(VerificationStatus.PENDING, created.verificationStatus)

        // Verify retrieval by ID
        val getResult = financialRepository.getMovementById(created.id)
        assertTrue("Fetching created movement by ID must succeed", getResult.isSuccess)
        val fetched = getResult.getOrNull()
        assertNotNull(fetched)
        assertEquals(created.id, fetched!!.id)
        assertEquals(amount, fetched.amount, 0.01)

        // Verify newly created movement appears in all movements list
        val allResult = financialRepository.getAllMovements()
        assertTrue(allResult.isSuccess)
        val list = allResult.getOrNull() ?: emptyList()
        assertTrue("Created movement must appear in all movements list", list.any { it.id == created.id })
    }

    @Test
    fun testVerifyMovement_and_RejectMovement_contracts() = runBlocking {
        // 1. Verify a DEBIT movement
        val createVrf = financialRepository.createMovement(
            customerId = testCustomerId,
            amount = 800.0,
            type = MovementType.DEBIT,
            description = "Movement to verify"
        )
        assertTrue(createVrf.isSuccess)
        val vrfMov = createVrf.getOrNull()!!
        disposableMovementIds.add(vrfMov.id)

        val verifyResult = financialRepository.verifyMovement(vrfMov.id, "Verified by test suite")
        assertTrue("Verifying movement must succeed: ${verifyResult.exceptionOrNull()?.message}", verifyResult.isSuccess)
        val verified = verifyResult.getOrNull()
        assertNotNull(verified)
        assertEquals(VerificationStatus.VERIFIED, verified!!.verificationStatus)
        assertEquals("Verified by test suite", verified.verificationNote)

        // 2. Reject a CREDIT movement
        val createRej = financialRepository.createMovement(
            customerId = testCustomerId,
            amount = 300.0,
            type = MovementType.CREDIT,
            description = "Movement to reject"
        )
        assertTrue(createRej.isSuccess)
        val rejMov = createRej.getOrNull()!!
        disposableMovementIds.add(rejMov.id)

        val rejectResult = financialRepository.rejectMovement(rejMov.id, "Invalid receipt submitted")
        assertTrue("Rejecting movement must succeed: ${rejectResult.exceptionOrNull()?.message}", rejectResult.isSuccess)
        val rejected = rejectResult.getOrNull()
        assertNotNull(rejected)
        assertEquals(VerificationStatus.REJECTED, rejected!!.verificationStatus)
        assertEquals("Invalid receipt submitted", rejected.verificationNote)
    }

    @Test
    fun testCustomerBalanceEndpoint() = runBlocking {
        // Record a DEBIT of 1000 ETB and a CREDIT payment of 400 ETB
        val deb = financialRepository.createMovement(testCustomerId, 1000.0, MovementType.DEBIT, "Balance Test Debit")
        assertTrue(deb.isSuccess)
        disposableMovementIds.add(deb.getOrNull()!!.id)

        val cred = financialRepository.createMovement(testCustomerId, 400.0, MovementType.CREDIT, "Balance Test Credit")
        assertTrue(cred.isSuccess)
        disposableMovementIds.add(cred.getOrNull()!!.id)

        // Query customer balance from backend
        val response = apiService.getCustomerBalance(testCustomerId.toLong())
        assertTrue("Fetching customer balance must succeed", response.isSuccessful)
        val balDto = response.body()?.data
        assertNotNull("Balance payload must exist", balDto)
        assertEquals(1000.0, balDto!!.totalDebit, 0.01)
        assertEquals(400.0, balDto.totalCredit, 0.01)
        // Formula: balance = total_credit - total_debit = 400 - 1000 = -600 (customer owes 600)
        assertEquals(-600.0, balDto.balance, 0.01)
    }

    @Test
    fun testUpdateMovement_resetsVerificationToPending() = runBlocking {
        val createResult = financialRepository.createMovement(testCustomerId, 500.0, MovementType.DEBIT, "Original entry")
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!
        disposableMovementIds.add(created.id)

        // Verify movement
        financialRepository.verifyMovement(created.id, "Initial verification")

        // Update movement
        val updateResult = financialRepository.updateMovement(
            movementId = created.id,
            customerId = testCustomerId,
            amount = 750.0,
            type = MovementType.DEBIT,
            description = "Updated entry"
        )
        assertTrue("Updating movement must succeed: ${updateResult.exceptionOrNull()?.message}", updateResult.isSuccess)
        val updated = updateResult.getOrNull()!!
        assertEquals(750.0, updated.amount, 0.01)
        assertEquals("Updated entry", updated.description)
        // Per backend rules, modifying a movement resets verification status to PENDING
        assertEquals(VerificationStatus.PENDING, updated.verificationStatus)
    }

    @Test
    fun testDeleteMovement_removesRecord() = runBlocking {
        val createResult = financialRepository.createMovement(testCustomerId, 200.0, MovementType.CREDIT, "To delete")
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!

        val deleteResult = financialRepository.deleteMovement(created.id)
        assertTrue("Delete movement must succeed: ${deleteResult.exceptionOrNull()?.message}", deleteResult.isSuccess)

        val getResult = financialRepository.getMovementById(created.id)
        assertTrue("Lookup after deletion must fail", getResult.isFailure)
    }

    @Test
    fun testFinanceViewModel_lifecycle_and_balanceGrouping() = runBlocking {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = FinanceViewModel(
            financialRepository = financialRepository,
            customerRepository = customerRepository
        )

        var successState: FinanceUiState.Success? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            val st = vm.uiState.value
            if (st is FinanceUiState.Success) {
                successState = st
                break
            }
        }
        assertNotNull("FinanceViewModel uiState must be Success", successState)

        // Search test
        vm.onSearchQueryChanged("non_existent_search_query_123")
        val searched = vm.uiState.value as FinanceUiState.Success
        assertTrue("Filtered debt list must be empty for non-existent query", searched.customersWhoOwe.isEmpty())

        vm.onSearchQueryChanged("")
        val cleared = vm.uiState.value as FinanceUiState.Success
        assertNotNull(cleared.customersWhoOwe)
    }

    @Test
    fun testFinancialMovementsViewModel_filtering() = runBlocking {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = FinancialMovementsViewModel(
            financialRepository = financialRepository,
            externalScope = testScope
        )

        var state: FinancialMovementsUiState.Success? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            val st = vm.uiState.value
            if (st is FinancialMovementsUiState.Success) {
                state = st
                break
            }
        }
        assertNotNull("FinancialMovementsViewModel state must be Success", state)

        // Filter DEBIT type
        vm.onTypeFilterChanged(MovementType.DEBIT)
        val filteredState = vm.uiState.value as FinancialMovementsUiState.Success
        assertEquals(MovementType.DEBIT, filteredState.selectedTypeFilter)
        assertTrue(filteredState.filteredMovements.all { it.type == MovementType.DEBIT })
    }

    @Test
    fun testFinancialMovementDetailsViewModel_load_and_verify() = runBlocking {
        val createResult = financialRepository.createMovement(testCustomerId, 900.0, MovementType.DEBIT, "VM Verify Test")
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()!!
        disposableMovementIds.add(created.id)

        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = FinancialMovementDetailsViewModel(
            financialRepository = financialRepository,
            externalScope = testScope
        )
        vm.loadMovement(created.id)

        var loadedState: FinancialMovementDetailsUiState.Success? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            val st = vm.uiState.value
            if (st is FinancialMovementDetailsUiState.Success) {
                loadedState = st
                break
            }
        }
        assertNotNull("Details state must be Success", loadedState)
        assertEquals(created.id, loadedState!!.movement.id)

        // Verify movement
        vm.verifyMovement(created.id, "Verified in test")
        var verifiedState: FinancialMovementDetailsUiState.Success? = null
        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            val st = vm.uiState.value
            if (st is FinancialMovementDetailsUiState.Success && !st.isSubmitting && st.successMessage != null) {
                verifiedState = st
                break
            }
        }
        assertNotNull("Verified state must exist", verifiedState)
        assertEquals(VerificationStatus.VERIFIED, verifiedState!!.movement.verificationStatus)
    }

    @Test
    fun testRecordFinancialMovementViewModel_validation_and_submission() = runBlocking {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val vm = RecordFinancialMovementViewModel(
            financialRepository = financialRepository,
            customerRepository = customerRepository,
            externalScope = testScope
        )

        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            if (vm.uiState.value.customers.isNotEmpty()) break
        }

        // 1. Validation test with null customer
        vm.submitTransaction(selectedCustomer = null, movementType = MovementType.DEBIT, amount = 100.0, description = "Test") {}
        assertNotNull("Error message must be set when customer is null", vm.uiState.value.errorMessage)

        vm.clearError()
        assertNull("Error message must be cleared", vm.uiState.value.errorMessage)

        // 2. Successful transaction submission
        val customerResult = customerRepository.getCustomers()
        val customer = customerResult.getOrNull()?.firstOrNull()
        assertNotNull("Customer must exist for submission test", customer)

        var successCallback = false
        vm.submitTransaction(
            selectedCustomer = customer,
            movementType = MovementType.CREDIT,
            amount = 250.0,
            description = "Test Payment Credit Submission"
        ) {
            successCallback = true
        }

        for (i in 1..20) {
            kotlinx.coroutines.delay(100)
            if (vm.uiState.value.isSuccess) break
        }
        assertTrue("Success callback must be called", successCallback)
        assertTrue("isSuccess must be true", vm.uiState.value.isSuccess)
    }
}
