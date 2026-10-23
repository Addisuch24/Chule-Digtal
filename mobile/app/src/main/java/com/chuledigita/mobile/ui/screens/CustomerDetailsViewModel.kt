package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CustomerDetailsUiState {
    data object Loading : CustomerDetailsUiState
    data class Success(
        val customer: Customer,
        val notes: List<CustomerNote> = emptyList()
    ) : CustomerDetailsUiState
    data class Error(val message: String) : CustomerDetailsUiState
}

class CustomerDetailsViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerDetailsUiState>(CustomerDetailsUiState.Loading)
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCustomerDetails()
    }

    fun loadCustomerDetails() {
        viewModelScope.launch {
            _uiState.value = CustomerDetailsUiState.Loading
            val customerResult = customerRepository.getCustomerById(customerId)
            customerResult.fold(
                onSuccess = { customer ->
                    val notesResult = customerRepository.getCustomerNotes(customerId)
                    val notes = notesResult.getOrDefault(emptyList())
                    _uiState.value = CustomerDetailsUiState.Success(
                        customer = customer,
                        notes = notes
                    )
                },
                onFailure = { error ->
                    _uiState.value = CustomerDetailsUiState.Error(
                        error.message ?: "Failed to load customer profile from server"
                    )
                }
            )
        }
    }

    fun addNote(noteText: String, onComplete: (Boolean, String) -> Unit) {
        if (noteText.isBlank()) return
        viewModelScope.launch {
            val result = customerRepository.createCustomerNote(customerId, noteText.trim())
            result.fold(
                onSuccess = { newNote ->
                    val currentState = _uiState.value
                    if (currentState is CustomerDetailsUiState.Success) {
                        _uiState.value = currentState.copy(
                            notes = listOf(newNote) + currentState.notes
                        )
                    }
                    onComplete(true, "ማስታወሻ በተሳካ ሁኔታ ተቀምጧል")
                },
                onFailure = { error ->
                    onComplete(false, error.message ?: "Failed to save note")
                }
            )
        }
    }

    fun deleteCustomer(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = customerRepository.deleteCustomer(customerId)
            result.fold(
                onSuccess = {
                    onComplete(true, "ደንበኛው በተሳካ ሁኔታ ተሰርዟል።")
                },
                onFailure = { error ->
                    onComplete(false, error.message ?: "ደንበኛውን መሰረዝ አልተቻለም።")
                }
            )
        }
    }

    companion object {
        fun provideFactory(customerId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                val customerRepository = application.container.customerRepository
                CustomerDetailsViewModel(customerId, customerRepository)
            }
        }
    }
}
