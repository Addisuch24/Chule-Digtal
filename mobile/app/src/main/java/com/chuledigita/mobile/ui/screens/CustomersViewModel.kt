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
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Customers screen, managing real backend customer records and real-time Socket.IO events.
 */
class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val notificationRepository: NotificationRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomersUiState>(CustomersUiState.Loading)
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()

        if (notificationRepository != null) {
            viewModelScope.launch {
                notificationRepository.realtimeEventFlow.collect { event ->
                    if (event.type == NotificationType.CUSTOMER_CREATED ||
                        event.type == NotificationType.CUSTOMER_NOTE_CREATED) {
                        loadCustomers()
                    }
                }
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.value = CustomersUiState.Loading
            val result = customerRepository.getCustomers()
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = CustomersUiState.Empty
                    } else {
                        val current = _uiState.value
                        if (current is CustomersUiState.Success) {
                            _uiState.value = current.copy(customers = list)
                        } else {
                            _uiState.value = CustomersUiState.Success(customers = list)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = CustomersUiState.Error(
                        error.message ?: "Failed to fetch customers from server."
                    )
                }
            )
        }
    }

    fun refresh() {
        loadCustomers()
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

    fun createCustomer(
        name: String,
        phone: String?,
        note: String?,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            val result = customerRepository.createCustomer(name = name, phone = phone)
            result.fold(
                onSuccess = { createdCustomer ->
                    if (!note.isNullOrBlank()) {
                        customerRepository.createCustomerNote(createdCustomer.id, note.trim())
                    }
                    refresh()
                    onComplete(true, "ደንበኛ በተሳካ ሁኔታ ተመዝግቧል")
                },
                onFailure = { error ->
                    onComplete(false, error.message ?: "Failed to create customer")
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                CustomersViewModel(
                    customerRepository = application.container.customerRepository,
                    notificationRepository = application.container.notificationRepository
                )
            }
        }
    }
}
