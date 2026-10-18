package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.FinancialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordFinancialMovementUiState(
    val customers: List<Customer> = emptyList(),
    val isLoadingCustomers: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class RecordFinancialMovementViewModel(
    private val financialRepository: FinancialRepository,
    private val customerRepository: CustomerRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(RecordFinancialMovementUiState())
    val uiState: StateFlow<RecordFinancialMovementUiState> = _uiState.asStateFlow()

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
                    errorMessage = error.message ?: "Failed to load customers"
                )
            }
        }
    }

    fun submitTransaction(
        selectedCustomer: Customer?,
        movementType: MovementType,
        amount: Double,
        description: String,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isSubmitting) return

        if (selectedCustomer == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a customer.")
            return
        }
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Amount must be greater than 0.")
            return
        }
        if (description.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Description is required.")
            return
        }

        scope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val result = financialRepository.createMovement(
                customerId = selectedCustomer.id,
                amount = amount,
                type = movementType,
                description = description.trim()
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
                onSuccess()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Failed to record transaction."
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
                RecordFinancialMovementViewModel(
                    financialRepository = application.container.financialRepository,
                    customerRepository = application.container.customerRepository
                )
            }
        }
    }
}
