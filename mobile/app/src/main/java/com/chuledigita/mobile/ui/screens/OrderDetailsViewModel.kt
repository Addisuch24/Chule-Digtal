package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrderDetailsUiState {
    data object Loading : OrderDetailsUiState
    data class Success(
        val order: Order,
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false
    ) : OrderDetailsUiState
    data class Error(val message: String) : OrderDetailsUiState
}

class OrderDetailsViewModel(
    private val _orderRepository: OrderRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val uiState: StateFlow<OrderDetailsUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        scope.launch {
            _uiState.value = OrderDetailsUiState.Loading
            val result = _orderRepository.getOrderById(orderId)
            result.onSuccess { order ->
                _uiState.value = OrderDetailsUiState.Success(order)
            }.onFailure { error ->
                _uiState.value = OrderDetailsUiState.Error(
                    error.message ?: "የትዕዛዝ ዝርዝር ማምጣት አልተቻለም።"
                )
            }
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        val currentState = _uiState.value as? OrderDetailsUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            if (currentState != null) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }
            val result = _orderRepository.updateOrderStatus(orderId, status)
            result.onSuccess { updatedOrder ->
                val msg = if (status == OrderStatus.COMPLETED) "ትዕዛዙ በተሳካ ሁኔታ ተጠናቋል።" else "የትዕዛዝ ሁኔታ ተቀይሯል።"
                _uiState.value = OrderDetailsUiState.Success(
                    order = updatedOrder,
                    successMessage = msg,
                    errorMessage = null,
                    isSubmitting = false
                )
            }.onFailure { error ->
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        errorMessage = error.message ?: "የትዕዛዝ ሁኔታ መቀየር አልተቻለም።",
                        successMessage = null,
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun verifyOrder(orderId: String, note: String? = null) {
        val currentState = _uiState.value as? OrderDetailsUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        scope.launch {
            if (currentState != null) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }
            val result = _orderRepository.verifyOrder(orderId, note)
            result.onSuccess { verifiedOrder ->
                _uiState.value = OrderDetailsUiState.Success(
                    order = verifiedOrder,
                    successMessage = "ትዕዛዝ በተሳካ ሁኔታ ተረጋግጧል።",
                    errorMessage = null,
                    isSubmitting = false
                )
            }.onFailure { error ->
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        errorMessage = error.message ?: "ትዕዛዝ ማረጋገጥ አልተቻለም።",
                        successMessage = null,
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun rejectOrder(orderId: String, note: String) {
        val currentState = _uiState.value as? OrderDetailsUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        if (note.isBlank()) {
            if (currentState != null) {
                _uiState.value = currentState.copy(
                    errorMessage = "ትዕዛዝ ውድቅ ለማድረግ ምክንያቱን መግለጽ ግዴታ ነው።",
                    isSubmitting = false
                )
            }
            return
        }

        scope.launch {
            if (currentState != null) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }
            val result = _orderRepository.rejectOrder(orderId, note.trim())
            result.onSuccess { rejectedOrder ->
                _uiState.value = OrderDetailsUiState.Success(
                    order = rejectedOrder,
                    successMessage = "ትዕዛዝ ውድቅ ተደርጓል።",
                    errorMessage = null,
                    isSubmitting = false
                )
            }.onFailure { error ->
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        errorMessage = error.message ?: "ትዕዛዝ ውድቅ ማድረግ አልተቻለም።",
                        successMessage = null,
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun clearMessage() {
        val currentState = _uiState.value as? OrderDetailsUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(successMessage = null, errorMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                OrderDetailsViewModel(_orderRepository = application.container.orderRepository)
            }
        }
    }
}
