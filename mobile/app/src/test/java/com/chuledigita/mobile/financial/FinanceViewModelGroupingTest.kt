package com.chuledigita.mobile.financial

import com.chuledigita.mobile.data.remote.model.BulkDeleteData
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.ui.screens.FinanceUiState
import com.chuledigita.mobile.ui.screens.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelGroupingTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCustomerRepo : CustomerRepository {
        val customers = mutableListOf<Customer>()

        override suspend fun getCustomers(): Result<List<Customer>> = Result.success(customers.toList())
        override suspend fun getCustomerById(customerId: String): Result<Customer> =
            customers.find { it.id == customerId }?.let { Result.success(it) }
                ?: Result.failure(Exception("Not found"))

        override suspend fun createCustomer(name: String, phone: String?): Result<Customer> {
            val existing = customers.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
            if (existing != null) {
                return Result.success(existing)
            }
            val newCust = Customer(
                id = (customers.size + 1).toString(),
                name = name.trim(),
                phone = phone,
                createdBy = "Admin",
                createdDate = "2026-10-22",
                updatedDate = "2026-10-22"
            )
            customers.add(newCust)
            return Result.success(newCust)
        }

        override suspend fun updateCustomer(customerId: String, name: String, phone: String?): Result<Customer> {
            val index = customers.indexOfFirst { it.id == customerId }
            if (index == -1) return Result.failure(Exception("Not found"))
            val updated = customers[index].copy(name = name, phone = phone)
            customers[index] = updated
            return Result.success(updated)
        }

        override suspend fun recordPayment(customerId: String, amount: Double, description: String?, idempotencyKey: String?): Result<Unit> =
            Result.success(Unit)

        override suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>> =
            Result.success(emptyList())

        override suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote> =
            Result.success(CustomerNote(id = "n1", customerId = customerId, note = text, date = "2026-10-22", authorName = "Admin"))

        override suspend fun deleteCustomer(customerId: String): Result<Unit> {
            customers.removeAll { it.id == customerId }
            return Result.success(Unit)
        }

        override suspend fun bulkDeleteCustomers(customerIds: List<String>): Result<BulkDeleteData> {
            customers.removeAll { it.id in customerIds }
            return Result.success(BulkDeleteData(deletedCount = customerIds.size, deletedIds = customerIds.map { it.toLong() }))
        }
    }

    private class FakeFinancialRepo : FinancialRepository {
        val movements = mutableListOf<FinancialMovement>()

        override suspend fun getAllMovements(): Result<List<FinancialMovement>> = Result.success(movements.toList())
        override suspend fun getMovementById(movementId: String): Result<FinancialMovement> =
            movements.find { it.id == movementId }?.let { Result.success(it) }
                ?: Result.failure(Exception("Not found"))

        override suspend fun createMovement(
            customerId: String,
            amount: Double,
            type: MovementType,
            description: String
        ): Result<FinancialMovement> {
            val mov = FinancialMovement(
                id = (movements.size + 1).toString(),
                customerId = customerId,
                customerName = "Addisu",
                type = type,
                amount = amount,
                date = "2026-10-22",
                description = description,
                recordedBy = "Admin",
                verificationStatus = VerificationStatus.VERIFIED
            )
            movements.add(mov)
            return Result.success(mov)
        }

        override suspend fun updateMovement(
            movementId: String,
            customerId: String,
            amount: Double,
            type: MovementType,
            description: String
        ): Result<FinancialMovement> {
            val index = movements.indexOfFirst { it.id == movementId }
            if (index == -1) return Result.failure(Exception("Not found"))
            val updated = movements[index].copy(amount = amount, type = type, description = description)
            movements[index] = updated
            return Result.success(updated)
        }

        override suspend fun deleteMovement(movementId: String): Result<Unit> {
            movements.removeAll { it.id == movementId }
            return Result.success(Unit)
        }

        override suspend fun bulkDeleteMovements(movementIds: List<String>): Result<BulkDeleteData> {
            movements.removeAll { it.id in movementIds }
            return Result.success(BulkDeleteData(deletedCount = movementIds.size, deletedIds = movementIds.map { it.toLong() }))
        }

        override suspend fun verifyMovement(movementId: String, note: String): Result<FinancialMovement> =
            Result.failure(Exception("Not implemented"))

        override suspend fun rejectMovement(movementId: String, note: String): Result<FinancialMovement> =
            Result.failure(Exception("Not implemented"))
    }

    private lateinit var customerRepo: FakeCustomerRepo
    private lateinit var financialRepo: FakeFinancialRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        customerRepo = FakeCustomerRepo()
        financialRepo = FakeFinancialRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMultipleBalanceRecords_underOneCustomer_countedAsOnlyOneCustomer() = runTest {
        // 1 Customer exists
        customerRepo.customers.add(
            Customer(id = "1", name = "Addisu", phone = "0911223344", createdBy = "Admin", createdDate = "2026-10-22", updatedDate = "2026-10-22")
        )

        // Multiple balance records exist for this same customer: 2 Debits and 1 Credit
        financialRepo.movements.addAll(
            listOf(
                FinancialMovement("101", "1", "Addisu", MovementType.DEBIT, 1000.0, "2026-10-20", "Debit 1", "Admin", VerificationStatus.VERIFIED),
                FinancialMovement("102", "1", "Addisu", MovementType.CREDIT, 400.0, "2026-10-21", "Credit 1", "Admin", VerificationStatus.VERIFIED),
                FinancialMovement("103", "1", "Addisu", MovementType.DEBIT, 200.0, "2026-10-22", "Debit 2", "Admin", VerificationStatus.VERIFIED)
            )
        )

        val viewModel = FinanceViewModel(financialRepo, customerRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as FinanceUiState.Success
        // Customer total movements is 3, but the customer list has ONLY 1 customer!
        assertEquals("Total movements must be 3", 3, state.totalMovementCount)
        assertEquals("Debtors list must contain exactly 1 customer", 1, state.customersWhoOwe.size)
        assertEquals("Creditors list must be empty", 0, state.customersWithCredit.size)

        val customerBalance = state.customersWhoOwe.first()
        assertEquals("Addisu", customerBalance.customerName)
        // Net balance: 400 (credit) - (1000 + 200) (debit) = -800
        assertEquals(-800.0, customerBalance.balance, 0.001)
        assertEquals("Customer balance must hold all 3 individual movement records", 3, customerBalance.movements.size)

        // Now test: recording a new entry for "Addisu" must NOT increase the customer count!
        viewModel.recordEntry("Addisu", "0911223344", -500.0, "New debit entry")
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterEntry = viewModel.uiState.value as FinanceUiState.Success
        assertEquals("Debtors list must STILL contain only 1 customer", 1, stateAfterEntry.customersWhoOwe.size)
        val updatedBalance = stateAfterEntry.customersWhoOwe.first()
        assertEquals("Addisu", updatedBalance.customerName)
        assertEquals(-1300.0, updatedBalance.balance, 0.001)
        assertEquals("Customer balance must now hold 4 individual movement records", 4, updatedBalance.movements.size)

        // Now test: updating an existing movement must NOT duplicate customer
        viewModel.updateMovement("101", "1", -1500.0, "Updated Debit 1")
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterUpdate = viewModel.uiState.value as FinanceUiState.Success
        assertEquals("Debtors list must STILL contain only 1 customer after edit", 1, stateAfterUpdate.customersWhoOwe.size)
    }
}
