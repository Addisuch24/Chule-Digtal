package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.Customer

/**
 * UI state for the Customers screen, supporting loading, success, empty, and error states.
 */
sealed interface CustomersUiState {
    data object Loading : CustomersUiState
    
    data class Success(
        val customers: List<Customer>,
        val searchQuery: String = "",
        val selectedFilter: CustomerFilter = CustomerFilter.ALL,
        val selectedSort: CustomerSort = CustomerSort.NAME
    ) : CustomersUiState

    data object Empty : CustomersUiState
    
    data class Error(val message: String) : CustomersUiState
}

enum class CustomerFilter {
    ALL,
    WITH_BALANCE,
    NO_BALANCE
}

enum class CustomerSort {
    NAME,
    BALANCE,
    NEWEST
}
