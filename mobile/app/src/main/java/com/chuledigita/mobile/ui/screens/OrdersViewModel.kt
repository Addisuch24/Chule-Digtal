package com.chuledigita.mobile.ui.screens

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.OrderRepository
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing the Orders tab logic with local sample data to avoid hanging on loading state.
 * Now reactively observes MockDataProvider for customers and orders.
 */
class OrdersViewModel(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            // Reactively sync customers from MockDataProvider
            snapshotFlow { MockDataProvider.customers.toList() }
                .collect { list ->
                    _customers.value = list
                }
        }

        viewModelScope.launch {
            // Reactively sync orders from MockDataProvider
            snapshotFlow { MockDataProvider.orders.toList() }
                .collect { allOrders ->
                    val active = allOrders.filter { 
                        it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED 
                    }
                    val completed = allOrders.filter { it.status == OrderStatus.COMPLETED }
                    
                    _uiState.value = OrdersUiState.Success(
                        activeOrders = active,
                        completedOrders = completed
                    )
                }
        }
    }

    fun loadData() {
        // Data is now reactively loaded via observeData()
    }

    /**
     * Registers a new order mapping UI fields locally into the in-memory sample collection.
     */
    fun createOrder(customerId: String, title: String, details: String, size: String) {
        viewModelScope.launch {
            val combinedDescription = "ዝርዝር: $details\nመጠን: $size"
            val uniqueOrderNumber = "$title-${System.currentTimeMillis().toString().takeLast(4)}"
            val customerName = MockDataProvider.getCustomerById(customerId)?.name ?: "የማይታወቅ ደንበኛ"

            val newOrder = Order(
                id = UUID.randomUUID().toString(),
                orderNumber = uniqueOrderNumber,
                customerId = customerId,
                customerName = customerName,
                description = combinedDescription,
                orderDate = "ዛሬ",
                orderTime = "አሁን",
                totalAmount = 0.0,
                status = OrderStatus.PENDING,
                verificationStatus = VerificationStatus.PENDING,
                takenByFamilyMember = MockDataProvider.currentFamilyMember.name
            )
            
            MockDataProvider.addOrder(newOrder)

            val currentState = _uiState.value
            if (currentState is OrdersUiState.Success) {
                _uiState.value = currentState.copy(successMessage = "ትዕዛዙ በተሳካ ሁኔታ ተመዝግቧል።")
            }
        }
    }

    fun resetSuccessMessage() {
        val currentState = _uiState.value
        if (currentState is OrdersUiState.Success) {
            _uiState.value = currentState.copy(successMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                OrdersViewModel(
                    orderRepository = application.container.orderRepository,
                    customerRepository = application.container.customerRepository
                )
            }
        }
    }
}
