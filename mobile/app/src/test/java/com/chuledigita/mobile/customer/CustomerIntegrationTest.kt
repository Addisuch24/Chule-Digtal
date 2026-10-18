package com.chuledigita.mobile.customer

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.AuthInterceptor
import com.chuledigita.mobile.data.remote.model.*
import com.chuledigita.mobile.data.repository.AuthRepositoryImpl
import com.chuledigita.mobile.data.repository.CustomerRepositoryImpl
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.SessionRepository
import com.chuledigita.mobile.ui.screens.DashboardUiState
import com.chuledigita.mobile.ui.screens.DashboardViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CustomerIntegrationTest {

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
    private lateinit var customerRepository: CustomerRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl

    private val localApiBaseUrl = "http://127.0.0.1:5000/api/"

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

        // Authenticate with a test account so all subsequent calls carry the real JWT
        val username = "integ_test_user_${java.util.UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        val reg = authRepository.register("Integ Test", null, "$username@gmail.com", username, "testPass123")
        assertTrue("Test registration must succeed: ${reg.exceptionOrNull()?.message}", reg.isSuccess)
        val login = authRepository.login(username, "testPass123")
        assertTrue("Test login must succeed: ${login.exceptionOrNull()?.message}", login.isSuccess)
        assertNotNull("Session token must be present", fakeSessionRepository.getCachedToken())
    }

    @Test
    fun testDashboardSummary_returnsRealMetrics() = runBlocking {
        val response = apiService.getDashboardSummary()
        assertTrue("GET /dashboard must succeed", response.isSuccessful)
        assertEquals(200, response.code())
        val body = response.body()
        assertNotNull(body)
        assertTrue(body!!.success)

        val data = body.data
        assertNotNull("Dashboard data must not be null", data)
        assertNotNull("customers must exist", data!!.customers)
        assertTrue("Total customers must be >= 0", data.customers!!.total >= 0)
        assertNotNull("financialMovements must exist", data.financialMovements)
        assertNotNull("recentOrders list must exist", data.recentOrders)
        assertNotNull("recentNotes list must exist", data.recentNotes)
    }

    @Test
    fun testGetAllCustomers_returnsRealCustomerList() = runBlocking {
        val result = customerRepository.getCustomers()
        assertTrue("Customer repository getCustomers must succeed", result.isSuccess)
        val customers = result.getOrNull()
        assertNotNull(customers)
        assertTrue("Customer list should contain existing registered customers", customers!!.isNotEmpty())

        val first = customers.first()
        assertTrue("Customer ID must be non-empty", first.id.isNotBlank())
        assertTrue("Customer name must be non-empty", first.name.isNotBlank())
    }

    @Test
    fun testGetCustomerById_returnsCustomerAndBalance() = runBlocking {
        val listResult = customerRepository.getCustomers()
        assertTrue(listResult.isSuccess)
        val firstCustomer = listResult.getOrNull()?.firstOrNull()
        assertNotNull("At least one customer must exist", firstCustomer)

        val detailResult = customerRepository.getCustomerById(firstCustomer!!.id)
        assertTrue("getCustomerById must succeed: ${detailResult.exceptionOrNull()?.message}", detailResult.isSuccess)
        val detail = detailResult.getOrNull()
        assertNotNull(detail)
        assertEquals(firstCustomer.id, detail!!.id)
        assertEquals(firstCustomer.name, detail.name)
    }

    @Test
    fun testCustomerValidation_emptyName_fails() = runBlocking {
        val result = customerRepository.createCustomer("", null)
        assertTrue("Creating customer with empty name must fail", result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message ?: ""
        assertTrue(
            "Error message should mention customer name is required: $errorMsg",
            errorMsg.contains("name is required", ignoreCase = true)
        )
    }

    @Test
    fun testUnauthorizedRequest_withoutToken_returns401() = runBlocking {
        val savedToken = fakeSessionRepository.getCachedToken()
        try {
            fakeSessionRepository.clearSession()
            val response = apiService.getAllCustomers()
            assertEquals("Request without token must be 401 Unauthorized", 401, response.code())
        } finally {
            if (savedToken != null) {
                fakeSessionRepository.saveToken(savedToken)
            }
        }
    }

    @Test
    fun testCustomerCreation_and_NoteAttachment_lifecycle() = runBlocking {
        val uniqueName = "DISPOSABLE_TEST_${System.currentTimeMillis()}"
        val phone = "0912000000"

        // 1. Create customer
        val createResult = customerRepository.createCustomer(uniqueName, phone)
        assertTrue("Creating test customer must succeed: ${createResult.exceptionOrNull()?.message}", createResult.isSuccess)
        val created = createResult.getOrNull()
        assertNotNull(created)
        assertEquals(uniqueName, created!!.name)
        assertEquals(phone, created.phone)

        // 2. Attach a customer note
        val noteResult = customerRepository.createCustomerNote(created.id, "Test note for $uniqueName")
        assertTrue("Attaching customer note must succeed: ${noteResult.exceptionOrNull()?.message}", noteResult.isSuccess)
        val note = noteResult.getOrNull()
        assertNotNull(note)
        assertEquals(created.id, note!!.customerId)
        assertTrue(note.note.contains("Test note"))

        // 3. Fetch notes list for customer
        val notesListResult = customerRepository.getCustomerNotes(created.id)
        assertTrue("Fetching notes for customer must succeed", notesListResult.isSuccess)
        val notes = notesListResult.getOrNull()
        assertNotNull(notes)
        assertTrue("Customer notes list must contain created note", notes!!.any { it.id == note.id })

        // 4. Verify customer appears in getAllCustomers list
        val allCustomersResult = customerRepository.getCustomers()
        assertTrue(allCustomersResult.isSuccess)
        val allCustomers = allCustomersResult.getOrNull() ?: emptyList()
        assertTrue("Newly created customer must appear in customer list", allCustomers.any { it.id == created.id })
    }
}
