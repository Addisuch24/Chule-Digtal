package com.chuledigita.mobile.multiselect

import com.chuledigita.mobile.data.remote.model.BulkDeleteData
import com.chuledigita.mobile.data.remote.model.SkippedItemDto
import com.chuledigita.mobile.domain.model.*
import com.chuledigita.mobile.domain.repository.*
import com.chuledigita.mobile.notifications.InAppNotificationManager
import com.chuledigita.mobile.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultiSelectDeleteTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Fake Repositories ---

    private class FakeCustomerRepository : CustomerRepository {
        var customers = mutableListOf(
            Customer(id = "1", name = "Abebe", createdBy = "admin", createdDate = "2026-01-01", updatedDate = "2026-01-01"),
            Customer(id = "2", name = "Almaz", createdBy = "admin", createdDate = "2026-01-02", updatedDate = "2026-01-02"),
            Customer(id = "3", name = "Chala", createdBy = "admin", createdDate = "2026-01-03", updatedDate = "2026-01-03")
        )
        var bulkDeleteCalls = mutableListOf<List<String>>()

        override suspend fun getCustomers(): Result<List<Customer>> = Result.success(customers)
        override suspend fun getCustomerById(customerId: String): Result<Customer> =
            customers.find { it.id == customerId }?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))
        override suspend fun createCustomer(name: String, phone: String?): Result<Customer> =
            Result.success(Customer(id = "4", name = name, createdBy = "admin", createdDate = "2026-01-04", updatedDate = "2026-01-04"))
        override suspend fun updateCustomer(customerId: String, name: String, phone: String?): Result<Customer> {
            val existing = customers.find { it.id == customerId }
            return if (existing != null) {
                val updated = existing.copy(name = name, phone = phone)
                val idx = customers.indexOf(existing)
                customers[idx] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        }
        override suspend fun recordPayment(customerId: String, amount: Double, description: String?, idempotencyKey: String?): Result<Unit> = Result.success(Unit)
        override suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>> = Result.success(emptyList())
        override suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote> =
            Result.success(CustomerNote(id = "n1", customerId = customerId, note = text, date = "2026-01-01", authorName = "admin"))

        override suspend fun deleteCustomer(customerId: String): Result<Unit> {
            customers.removeAll { it.id == customerId }
            return Result.success(Unit)
        }

        override suspend fun bulkDeleteCustomers(customerIds: List<String>): Result<BulkDeleteData> {
            bulkDeleteCalls.add(customerIds)
            customers.removeAll { it.id in customerIds }
            return Result.success(BulkDeleteData(deletedCount = customerIds.size, deletedIds = customerIds.map { it.toLong() }))
        }
    }

    private class FakeNoteRepository : NoteRepository {
        var notes = mutableListOf(
            CustomerNote(id = "101", customerId = "0", note = "First Note", date = "2026-01-01", authorName = "Dawit"),
            CustomerNote(id = "102", customerId = "0", note = "Second Note", date = "2026-01-02", authorName = "Dawit"),
            CustomerNote(id = "103", customerId = "0", note = "Third Note", date = "2026-01-03", authorName = "Dawit")
        )
        var bulkDeleteCalls = mutableListOf<List<String>>()

        override suspend fun getGlobalNotes(): Result<List<CustomerNote>> = Result.success(notes)
        override suspend fun createGlobalNote(text: String): Result<CustomerNote> =
            Result.success(CustomerNote(id = "104", customerId = "0", note = text, date = "2026-01-04", authorName = "Dawit"))
        override suspend fun updateGlobalNote(noteId: String, text: String): Result<CustomerNote> =
            Result.success(CustomerNote(id = noteId, customerId = "0", note = text, date = "2026-01-01", authorName = "Dawit"))
        override suspend fun deleteGlobalNote(noteId: String): Result<Unit> {
            notes.removeAll { it.id == noteId }
            return Result.success(Unit)
        }
        override suspend fun bulkDeleteGlobalNotes(noteIds: List<String>): Result<BulkDeleteData> {
            bulkDeleteCalls.add(noteIds)
            notes.removeAll { it.id in noteIds }
            return Result.success(BulkDeleteData(deletedCount = noteIds.size, deletedIds = noteIds.map { it.toLong() }))
        }
        override suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>> = Result.success(emptyList())
        override suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote> =
            Result.success(CustomerNote(id = "n1", customerId = customerId, note = text, date = "2026-01-01", authorName = "Dawit"))
    }

    private class FakeOrderRepository : OrderRepository {
        var orders = mutableListOf(
            Order(id = "201", orderNumber = "ORD-001", customerId = "1", customerName = "Abebe", description = "Order 1", orderDate = "2026-01-01", totalAmount = 100.0, status = OrderStatus.PENDING, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"),
            Order(id = "202", orderNumber = "ORD-002", customerId = "2", customerName = "Almaz", description = "Order 2", orderDate = "2026-01-02", totalAmount = 200.0, status = OrderStatus.CONFIRMED, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"),
            Order(id = "203", orderNumber = "ORD-003", customerId = "3", customerName = "Chala", description = "Order 3", orderDate = "2026-01-03", totalAmount = 300.0, status = OrderStatus.COMPLETED, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit")
        )
        var bulkDeleteCalls = mutableListOf<List<String>>()

        override suspend fun getOrders(): Result<List<Order>> = Result.success(orders)
        override suspend fun getOrderById(orderId: String): Result<Order> =
            orders.find { it.id == orderId }?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))
        override suspend fun createOrder(customerId: String, orderNumber: String, description: String?): Result<Order> =
            Result.success(Order(id = "204", orderNumber = orderNumber, customerId = customerId, customerName = "Abebe", description = description ?: "", orderDate = "2026-01-04", totalAmount = 50.0, status = OrderStatus.PENDING, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"))
        override suspend fun createOrderDirect(orderNumber: String, title: String, description: String, customerId: String?): Result<Order> =
            Result.success(Order(id = "205", orderNumber = orderNumber, title = title, customerId = customerId ?: "1", customerName = "Abebe", description = description, orderDate = "2026-01-04", totalAmount = 50.0, status = OrderStatus.PENDING, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"))
        override suspend fun updateOrder(orderId: String, title: String?, description: String?): Result<Order> =
            Result.success(Order(id = orderId, orderNumber = "ORD-001", customerId = "1", customerName = "Abebe", description = description ?: "", orderDate = "2026-01-01", totalAmount = 50.0, status = OrderStatus.PENDING, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"))
        override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order> =
            Result.success(Order(id = orderId, orderNumber = "ORD-001", customerId = "1", customerName = "Abebe", description = "", orderDate = "2026-01-01", totalAmount = 50.0, status = status, verificationStatus = VerificationStatus.NOT_REQUIRED, takenByFamilyMember = "Dawit"))
        override suspend fun verifyOrder(orderId: String, verificationNote: String?): Result<Order> = Result.success(orders.first())
        override suspend fun rejectOrder(orderId: String, verificationNote: String): Result<Order> = Result.success(orders.first())
        override suspend fun deleteOrder(orderId: String): Result<Unit> {
            orders.removeAll { it.id == orderId }
            return Result.success(Unit)
        }
        override suspend fun bulkDeleteOrders(orderIds: List<String>): Result<BulkDeleteData> {
            bulkDeleteCalls.add(orderIds)
            orders.removeAll { it.id in orderIds }
            return Result.success(BulkDeleteData(deletedCount = orderIds.size, deletedIds = orderIds.map { it.toLong() }))
        }
    }

    private class FakeFinancialRepository : FinancialRepository {
        var movements = mutableListOf(
            FinancialMovement(id = "301", customerId = "1", customerName = "Abebe", type = MovementType.DEBIT, amount = 100.0, date = "2026-01-01", description = "Movement 1", recordedBy = "admin", verificationStatus = VerificationStatus.PENDING),
            FinancialMovement(id = "302", customerId = "2", customerName = "Almaz", type = MovementType.CREDIT, amount = 200.0, date = "2026-01-02", description = "Movement 2", recordedBy = "admin", verificationStatus = VerificationStatus.VERIFIED),
            FinancialMovement(id = "303", customerId = "3", customerName = "Chala", type = MovementType.DEBIT, amount = 300.0, date = "2026-01-03", description = "Movement 3", recordedBy = "admin", verificationStatus = VerificationStatus.REJECTED)
        )
        var bulkDeleteCalls = mutableListOf<List<String>>()

        override suspend fun getAllMovements(): Result<List<FinancialMovement>> = Result.success(movements)
        override suspend fun getMovementById(movementId: String): Result<FinancialMovement> =
            movements.find { it.id == movementId }?.let { Result.success(it) } ?: Result.failure(Exception("Not found"))
        override suspend fun createMovement(customerId: String, amount: Double, type: MovementType, description: String): Result<FinancialMovement> =
            Result.success(movements.first())
        override suspend fun updateMovement(movementId: String, customerId: String, amount: Double, type: MovementType, description: String): Result<FinancialMovement> =
            Result.success(movements.first())
        override suspend fun deleteMovement(movementId: String): Result<Unit> {
            movements.removeAll { it.id == movementId }
            return Result.success(Unit)
        }
        override suspend fun bulkDeleteMovements(movementIds: List<String>): Result<BulkDeleteData> {
            bulkDeleteCalls.add(movementIds)
            val toDelete = mutableListOf<Long>()
            val skipped = mutableListOf<SkippedItemDto>()
            for (id in movementIds) {
                val item = movements.find { it.id == id }
                if (item?.verificationStatus == VerificationStatus.VERIFIED) {
                    skipped.add(SkippedItemDto(id = id.toLong(), reason = "Verified records cannot be deleted"))
                } else {
                    toDelete.add(id.toLong())
                    movements.removeAll { it.id == id }
                }
            }
            return Result.success(BulkDeleteData(deletedCount = toDelete.size, deletedIds = toDelete, skipped = skipped, skippedCount = skipped.size))
        }
        override suspend fun verifyMovement(movementId: String, note: String): Result<FinancialMovement> = Result.success(movements.first())
        override suspend fun rejectMovement(movementId: String, note: String): Result<FinancialMovement> = Result.success(movements.first())
    }

    private class FakeNotificationRepository : NotificationRepository {
        val notifsList = mutableListOf(
            NotificationItem(id = "401", title = "Alert 1", message = "Message 1", date = "2026-01-01", time = "10:00", isRead = false, type = NotificationType.CUSTOMER_CREATED),
            NotificationItem(id = "402", title = "Alert 2", message = "Message 2", date = "2026-01-02", time = "11:00", isRead = true, type = NotificationType.ORDER_CREATED)
        )
        private val _notifications = MutableStateFlow<List<NotificationItem>>(notifsList)
        override val notifications: StateFlow<List<NotificationItem>> = _notifications

        override val unreadCount: StateFlow<Int> = MutableStateFlow(1)
        override val realtimeEventFlow: SharedFlow<NotificationItem> = MutableSharedFlow()
        override val inAppNotificationManager: InAppNotificationManager = InAppNotificationManager()

        var bulkDeleteCalls = mutableListOf<List<String>>()

        override suspend fun refreshNotifications(): Result<Unit> = Result.success(Unit)
        override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
        override suspend fun markAllAsRead(): Result<Unit> = Result.success(Unit)
        override suspend fun bulkDeleteNotifications(notificationIds: List<String>): Result<BulkDeleteData> {
            bulkDeleteCalls.add(notificationIds)
            notifsList.removeAll { it.id in notificationIds }
            _notifications.value = notifsList.toList()
            return Result.success(BulkDeleteData(deletedCount = notificationIds.size, deletedIds = notificationIds.map { it.toLong() }))
        }
        override suspend fun deleteNotification(notificationId: String): Result<Unit> {
            notifsList.removeAll { it.id == notificationId }
            _notifications.value = notifsList.toList()
            return Result.success(Unit)
        }
        override suspend fun deleteAllNotifications(): Result<Unit> {
            notifsList.clear()
            _notifications.value = emptyList()
            return Result.success(Unit)
        }
        override fun startRealTimeSync() {}
        override fun stopRealTimeSync() {}
    }

    // --- Tests ---

    @Test
    fun testCustomerMultiSelectAndBulkDelete() = runBlocking {
        val repo = FakeCustomerRepository()
        val viewModel = CustomersViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(0, state.selectedIds.size)
        assertFalse(state.isDeleting)

        // Select one customer
        viewModel.toggleSelectCustomer("1")
        state = viewModel.uiState.value as CustomersUiState.Success
        assertTrue(state.selectedIds.contains("1"))
        assertEquals(1, state.selectedIds.size)

        // Select another
        viewModel.toggleSelectCustomer("2")
        state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(2, state.selectedIds.size)

        // Deselect first
        viewModel.toggleSelectCustomer("1")
        state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(1, state.selectedIds.size)
        assertFalse(state.selectedIds.contains("1"))
        assertTrue(state.selectedIds.contains("2"))

        // Select All
        viewModel.selectAllCustomers(listOf("1", "2", "3"))
        state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(3, state.selectedIds.size)

        // Clear Selection
        viewModel.clearCustomerSelection()
        state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(0, state.selectedIds.size)

        // Select two and Bulk Delete
        viewModel.toggleSelectCustomer("1")
        viewModel.toggleSelectCustomer("3")
        var callbackCalled = false
        viewModel.bulkDeleteSelectedCustomers { deleted, skipped, msg ->
            callbackCalled = true
            assertEquals(2, deleted)
            assertEquals(0, skipped)
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackCalled)
        assertEquals(listOf("1", "3"), repo.bulkDeleteCalls.first())
        state = viewModel.uiState.value as CustomersUiState.Success
        assertEquals(0, state.selectedIds.size)
        assertEquals(1, state.customers.size)
        assertEquals("2", state.customers.first().id)
    }

    @Test
    fun testNotesMultiSelectAndBulkDelete() = runBlocking {
        val repo = FakeNoteRepository()
        val viewModel = NotesViewModel(repo, kotlinx.coroutines.CoroutineScope(testDispatcher))

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as NotesUiState.Success
        assertEquals(0, state.selectedNoteIds.size)

        // Select note
        viewModel.toggleSelectNote("101")
        state = viewModel.uiState.value as NotesUiState.Success
        assertEquals(setOf("101"), state.selectedNoteIds)

        // Select all then deselect one
        viewModel.selectAllNotes(listOf("101", "102", "103"))
        state = viewModel.uiState.value as NotesUiState.Success
        assertEquals(3, state.selectedNoteIds.size)

        // Clear selection
        viewModel.clearNoteSelection()
        state = viewModel.uiState.value as NotesUiState.Success
        assertEquals(0, state.selectedNoteIds.size)

        // Select 2 notes and bulk delete
        viewModel.toggleSelectNote("101")
        viewModel.toggleSelectNote("102")
        viewModel.bulkDeleteSelectedNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as NotesUiState.Success
        assertEquals(0, state.selectedNoteIds.size)
        assertEquals(1, state.notes.size)
        assertEquals("103", state.notes.first().id)
        assertEquals(1, repo.bulkDeleteCalls.size)

        // Delete remaining note and verify transitions to Empty
        viewModel.toggleSelectNote("103")
        viewModel.bulkDeleteSelectedNotes()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("When all notes are deleted, UI state becomes Empty", viewModel.uiState.value is NotesUiState.Empty)
    }

    @Test
    fun testOrdersMultiSelectAndBulkDelete() = runBlocking {
        val orderRepo = FakeOrderRepository()
        val customerRepo = FakeCustomerRepository()
        val viewModel = OrdersViewModel(orderRepo, customerRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as OrdersUiState.Success
        assertEquals(0, state.selectedOrderIds.size)

        // Select order 201 and 202
        viewModel.toggleSelectOrder("201")
        viewModel.toggleSelectOrder("202")
        state = viewModel.uiState.value as OrdersUiState.Success
        assertEquals(setOf("201", "202"), state.selectedOrderIds)

        // Clear selection
        viewModel.clearOrderSelection()
        state = viewModel.uiState.value as OrdersUiState.Success
        assertEquals(0, state.selectedOrderIds.size)

        // Select and delete
        viewModel.toggleSelectOrder("203")
        viewModel.bulkDeleteSelectedOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as OrdersUiState.Success
        assertEquals(0, state.selectedOrderIds.size)
        assertEquals(2, state.orders.size)
        assertNull(state.orders.find { it.id == "203" })
    }

    @Test
    fun testFinancialMovementsSafeguardVerifiedCannotBeSelectedOrDeleted() = runBlocking {
        val repo = FakeFinancialRepository()
        val viewModel = FinancialMovementsViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as FinancialMovementsUiState.Success
        assertEquals(0, state.selectedMovementIds.size)

        // Attempt to select movement 302 which is VERIFIED
        viewModel.toggleSelectMovement("302")
        state = viewModel.uiState.value as FinancialMovementsUiState.Success

        // Safeguard: verified movement was NOT added to selectedMovementIds!
        assertFalse("Verified record must not be selected", state.selectedMovementIds.contains("302"))
        assertEquals(0, state.selectedMovementIds.size)
        assertNotNull("Safeguard error message must be set", state.actionError)
        assertTrue(state.actionError!!.contains("የተረጋገጡ"))

        // Select unverified movement 301 (PENDING)
        viewModel.toggleSelectMovement("301")
        state = viewModel.uiState.value as FinancialMovementsUiState.Success
        assertTrue(state.selectedMovementIds.contains("301"))
        assertEquals(1, state.selectedMovementIds.size)

        // Select All should only select unverified movements
        viewModel.selectAllMovements(listOf("301", "302", "303"))
        state = viewModel.uiState.value as FinancialMovementsUiState.Success
        // 302 is VERIFIED so it must be excluded; 301 and 303 are selectable
        assertEquals(setOf("301", "303"), state.selectedMovementIds)
        assertFalse(state.selectedMovementIds.contains("302"))

        // Bulk delete selected unverified movements
        viewModel.bulkDeleteSelectedMovements()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as FinancialMovementsUiState.Success
        assertEquals(0, state.selectedMovementIds.size)
        // 302 remains in movements because it is verified and protected!
        assertEquals(1, state.movements.size)
        assertEquals("302", state.movements.first().id)
        assertEquals(VerificationStatus.VERIFIED, state.movements.first().verificationStatus)
    }

    @Test
    fun testNotificationsMultiSelectAndBulkDelete() = runBlocking {
        val repo = FakeNotificationRepository()
        val viewModel = NotificationsViewModel(repo, kotlinx.coroutines.CoroutineScope(testDispatcher))

        testDispatcher.scheduler.advanceUntilIdle()

        // Select notification 401
        viewModel.toggleSelectNotification("401")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as NotificationsUiState.Success
        assertEquals(setOf("401"), state.selectedNotificationIds)

        // Bulk delete
        viewModel.bulkDeleteSelectedNotifications()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value as NotificationsUiState.Success
        assertEquals(0, state.selectedNotificationIds.size)
        assertEquals(1, state.notifications.size)
        assertEquals("402", state.notifications.first().id)
    }
}
