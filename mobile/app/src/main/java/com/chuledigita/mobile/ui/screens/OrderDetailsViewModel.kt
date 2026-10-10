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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrderDetailsUiState {
    data object Loading : OrderDetailsUiState
    data class Success(val order: Order, val successMessage: String? = null) : OrderDetailsUiState
    data class Error(val message: String) : OrderDetailsUiState
}

class OrderDetailsViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val uiState: StateFlow<OrderDetailsUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = OrderDetailsUiState.Loading
            orderRepository.getOrderById(orderId).fold(
                onSuccess = { order ->
                    _uiState.value = OrderDetailsUiState.Success(order)
                },
                onFailure = { exception ->
                    _uiState.value = OrderDetailsUiState.Error(exception.message ?: "Failed to load order details")
                }
            )
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is OrderDetailsUiState.Success) {
                orderRepository.updateOrderStatus(orderId, status).fold(
                    onSuccess = { updatedOrder ->
                        val msg = if (status == OrderStatus.COMPLETED) "ትዕዛዙ በተሳካ ሁኔታ ተጠናቋል።" else "የትዕዛዝ ሁኔታ ተቀይሯል።"
                        _uiState.value = OrderDetailsUiState.Success(updatedOrder, successMessage = msg)
                    },
                    onFailure = { exception ->
                        // In case of error, we might want to show a toast or stay in Success with error
                    }
                )
            }
        }
    }
    
    fun clearMessage() {
        val currentState = _uiState.value
        if (currentState is OrderDetailsUiState.Success) {
            _uiState.value = currentState.copy(successMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                OrderDetailsViewModel(orderRepository = application.container.orderRepository)
            }
        }
    }
}
