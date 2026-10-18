package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateOrderUiState(
    val customers: List<Customer> = emptyList(),
    val isLoadingCustomers: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * ViewModel managing the order creation screen form state and backend persistence.
 */
class CreateOrderViewModel(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCustomers = true, errorMessage = null)
            val result = customerRepository.getCustomers()
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(customers = list, isLoadingCustomers = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingCustomers = false,
                    errorMessage = error.message ?: "Failed to load customer list from server."
                )
            }
        }
    }

    fun submitOrder(
        title: String,
        description: String,
        selectedCustomer: Customer?,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isSubmitting) return

        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "A title is required for the order.")
            return
        }
        if (trimmedTitle.length > 150) {
            _uiState.value = _uiState.value.copy(errorMessage = "Order title cannot exceed 150 characters.")
            return
        }
        if (trimmedDesc.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Order details/description are required.")
            return
        }

        scope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val generatedOrderNumber = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
            val result = orderRepository.createOrderDirect(
                orderNumber = generatedOrderNumber,
                title = trimmedTitle,
                description = trimmedDesc,
                customerId = selectedCustomer?.id
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Failed to create order on server."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                CreateOrderViewModel(
                    orderRepository = application.container.orderRepository,
                    customerRepository = application.container.customerRepository
                )
            }
        }
    }
}
