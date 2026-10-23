package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.Order

/**
 * UI state for the Orders screen matching web and mobile tracking capabilities.
 */
sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    
    data class Success(
        val orders: List<Order>,
        val activeOrders: List<Order>,
        val completedOrders: List<Order>,
        val searchQuery: String = "",
        val editingOrder: Order? = null,
        val selectedOrderIds: Set<String> = emptySet(),
        val isSubmitting: Boolean = false,
        val isDeletingBatch: Boolean = false,
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val batchDeleteMessage: String? = null,
        val batchDeleteError: String? = null
    ) : OrdersUiState

    data class Error(val message: String) : OrdersUiState
}
