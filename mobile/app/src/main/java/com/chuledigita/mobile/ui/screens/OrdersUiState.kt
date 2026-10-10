package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.Order

/**
 * UI state for the Orders screen, supporting loading, success, and error states.
 */
sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    
    data class Success(
        val activeOrders: List<Order>,
        val completedOrders: List<Order>,
        val successMessage: String? = null
    ) : OrdersUiState

    data class Error(val message: String) : OrdersUiState
}
