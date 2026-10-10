package com.chuledigita.mobile.ui.screens

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CustomersUiState>(CustomersUiState.Loading)
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        observeCustomers()
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            // Reactive bridge: Automatically updates UI state whenever MockDataProvider.customers changes
            snapshotFlow { MockDataProvider.customers.toList() }
                .collect { list ->
                    if (list.isEmpty()) {
                        _uiState.value = CustomersUiState.Empty
                    } else {
                        // Preserve search/filter if already in success state
                        val currentState = _uiState.value
                        if (currentState is CustomersUiState.Success) {
                            _uiState.value = currentState.copy(customers = list)
                        } else {
                            _uiState.value = CustomersUiState.Success(customers = list)
                        }
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is CustomersUiState.Success) {
            _uiState.update { currentState.copy(searchQuery = query) }
        }
    }

    fun onFilterChanged(filter: CustomerFilter) {
        val currentState = _uiState.value
        if (currentState is CustomersUiState.Success) {
            _uiState.update { currentState.copy(selectedFilter = filter) }
        }
    }
    
    fun deleteCustomer(id: String) {
        // Logic handled in MockDataProvider, observed via snapshotFlow
        MockDataProvider.deleteCustomer(id)
    }

    fun refresh() {
        // snapshotFlow will handle the update if data changes
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CustomersViewModel()
            }
        }
    }
}
