package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.NotificationRepository
import com.chuledigita.mobile.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Orders tab logic connected to real backend endpoints and real-time Socket.IO events.
 */
class OrdersViewModel(
    private val _orderRepository: OrderRepository,
    private val _customerRepository: CustomerRepository,
    private val _notificationRepository: NotificationRepository? = null,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    private var allOrders: List<Order> = emptyList()
    private var currentSearchQuery: String = ""

    init {
        loadData()

        if (_notificationRepository != null) {
            scope.launch {
                _notificationRepository.realtimeEventFlow.collect { event ->
                    if (event.type == NotificationType.ORDER_CREATED ||
                        event.type == NotificationType.ORDER_VERIFIED ||
                        event.type == NotificationType.ORDER_REJECTED ||
                        event.type == NotificationType.ORDER_STATUS_UPDATED) {
                        loadData()
                    }
                }
            }
        }
    }

    fun loadData() {
        scope.launch {
            _uiState.value = OrdersUiState.Loading

            // Optionally pre-load real customer listings
            val custResult = _customerRepository.getCustomers()
            if (custResult.isSuccess) {
                _customers.value = custResult.getOrNull() ?: emptyList()
            }

            val result = _orderRepository.getOrders()
            result.onSuccess { orders ->
                allOrders = orders
                updateState()
            }.onFailure { error ->
                _uiState.value = OrdersUiState.Error(
                    error.message ?: "ትዕዛዞችን ማምጣት አልተቻለም። እባክዎ እንደገና ይሞክሩ።"
                )
            }
        }
    }

    private fun updateState(
        successMsg: String? = null,
        errorMsg: String? = null,
        editing: Order? = null,
        submitting: Boolean = false
    ) {
        val filtered = if (currentSearchQuery.isBlank()) {
            allOrders
        } else {
            val q = currentSearchQuery.trim().lowercase()
            allOrders.filter { o ->
                (o.title?.lowercase()?.contains(q) == true) ||
                o.orderNumber.lowercase().contains(q) ||
                o.customerName.lowercase().contains(q) ||
                o.description.lowercase().contains(q)
            }
        }

        val active = filtered.filter {
            it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED
        }
        val completed = filtered.filter { it.status == OrderStatus.COMPLETED }

        _uiState.value = OrdersUiState.Success(
            orders = filtered,
            activeOrders = active,
            completedOrders = completed,
            searchQuery = currentSearchQuery,
            editingOrder = editing,
            isSubmitting = submitting,
            successMessage = successMsg,
            errorMessage = errorMsg
        )
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        val currentState = _uiState.value
        if (currentState is OrdersUiState.Success) {
            updateState(
                successMsg = currentState.successMessage,
                errorMsg = currentState.errorMessage,
                editing = currentState.editingOrder,
                submitting = currentState.isSubmitting
            )
        }
    }

    fun createOrder(title: String, details: String, customerId: String? = null) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        if (title.isBlank() || details.isBlank()) {
            updateState(
                errorMsg = "እባክዎ የትዕዛዝ ርዕስ እና ዝርዝር መግለጫ ያስገቡ።",
                submitting = false
            )
            return
        }
        if (title.length > 150) {
            updateState(
                errorMsg = "የትዕዛዝ ርዕስ ከ150 ፊደላት መብለጥ የለበትም።",
                submitting = false
            )
            return
        }

        scope.launch {
            setSubmitting(true)
            val generatedOrderNumber = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
            val result = _orderRepository.createOrderDirect(
                orderNumber = generatedOrderNumber,
                title = title.trim(),
                description = details.trim(),
                customerId = customerId
            )

            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "ትዕዛዝ በተሳካ ሁኔታ ተመዝግቧል።",
                    errorMsg = null,
                    editing = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "ትዕዛዝ መፍጠር አልተቻለም።",
                    successMsg = null,
                    editing = currentState?.editingOrder,
                    submitting = false
                )
            }
        }
    }

    fun updateOrder(orderId: String, title: String?, description: String?) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            setSubmitting(true)
            val result = _orderRepository.updateOrder(orderId, title, description)
            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "ትዕዛዝ በተሳካ ሁኔታ ተስተካክሏል።",
                    errorMsg = null,
                    editing = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "ትዕዛዝ ማስተካከል አልተቻለም።",
                    successMsg = null,
                    editing = currentState?.editingOrder,
                    submitting = false
                )
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            setSubmitting(true)
            val result = _orderRepository.updateOrderStatus(orderId, status)
            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "የትዕዛዝ ሁኔታ ተቀይሯል።",
                    errorMsg = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "የትዕዛዝ ሁኔታ መቀየር አልተቻለም።",
                    successMsg = null,
                    submitting = false
                )
            }
        }
    }

    fun deleteOrder(orderId: String) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            setSubmitting(true)
            val result = _orderRepository.deleteOrder(orderId)
            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "ትዕዛዝ በተሳካ ሁኔታ ተሰርዟል።",
                    errorMsg = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "ትዕዛዝ መሰረዝ አልተቻለም።",
                    successMsg = null,
                    submitting = false
                )
            }
        }
    }

    fun verifyOrder(orderId: String, note: String? = null) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            setSubmitting(true)
            val result = _orderRepository.verifyOrder(orderId, note)
            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "ትዕዛዝ በተሳካ ሁኔታ ተረጋግጧል።",
                    errorMsg = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "ትዕዛዝ ማረጋገጥ አልተቻለም።",
                    successMsg = null,
                    submitting = false
                )
            }
        }
    }

    fun rejectOrder(orderId: String, note: String) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        if (note.isBlank()) {
            updateState(
                errorMsg = "ትዕዛዝ ውድቅ ለማድረግ ምክንያቱን መግለጽ ግዴታ ነው።",
                submitting = false
            )
            return
        }

        scope.launch {
            setSubmitting(true)
            val result = _orderRepository.rejectOrder(orderId, note.trim())
            result.onSuccess {
                val refreshResult = _orderRepository.getOrders()
                if (refreshResult.isSuccess) {
                    allOrders = refreshResult.getOrNull() ?: allOrders
                }
                updateState(
                    successMsg = "ትዕዛዝ ውድቅ ተደርጓል።",
                    errorMsg = null,
                    submitting = false
                )
            }.onFailure { error ->
                updateState(
                    errorMsg = error.message ?: "ትዕዛዝ ውድቅ ማድረግ አልተቻለም።",
                    successMsg = null,
                    submitting = false
                )
            }
        }
    }

    fun startEditing(order: Order) {
        updateState(editing = order)
    }

    fun cancelEditing() {
        updateState(editing = null)
    }

    fun clearMessage() {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(successMessage = null, errorMessage = null)
        }
    }

    private fun setSubmitting(submitting: Boolean) {
        val currentState = _uiState.value as? OrdersUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(isSubmitting = submitting)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                OrdersViewModel(
                    _orderRepository = application.container.orderRepository,
                    _customerRepository = application.container.customerRepository,
                    _notificationRepository = application.container.notificationRepository
                )
            }
        }
    }
}
