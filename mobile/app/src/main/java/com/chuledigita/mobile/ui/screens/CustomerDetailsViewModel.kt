package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CustomerDetailsUiState {
    data object Loading : CustomerDetailsUiState
    data class Success(
        val customer: Customer,
        val notes: List<CustomerNote>,
        val orders: List<Order>,
        val movements: List<FinancialMovement>,
        val activityHistory: List<String>
    ) : CustomerDetailsUiState
    data class Error(val message: String) : CustomerDetailsUiState
}

class CustomerDetailsViewModel(private val customerId: String) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomerDetailsUiState>(CustomerDetailsUiState.Loading)
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCustomerDetails()
    }

    private fun loadCustomerDetails() {
        viewModelScope.launch {
            delay(500)
            val customer = MockDataProvider.customers.find { it.id == customerId }
            if (customer != null) {
                val notes = MockDataProvider.customerNotes.filter { it.customerId == customerId }
                val orders = MockDataProvider.orders.filter { it.customerId == customerId }
                val movements = MockDataProvider.financialMovements.filter { 
                    it.description.contains(customer.name, ignoreCase = true) 
                }
                
                val history = mutableListOf<String>()
                history.add("Customer profile created on ${customer.registrationDate}")
                orders.forEach { history.add("Order #${it.id} placed on ${it.orderDate} - ${it.status}") }
                movements.forEach { history.add("${it.type} of ${it.amount} ETB recorded on ${it.date}") }
                
                _uiState.value = CustomerDetailsUiState.Success(
                    customer = customer,
                    notes = notes,
                    orders = orders,
                    movements = movements,
                    activityHistory = history.sortedByDescending { it } // Simple sort for mock
                )
            } else {
                _uiState.value = CustomerDetailsUiState.Error("Customer not found.")
            }
        }
    }

    fun addNote(noteText: String) {
        val currentState = _uiState.value
        if (currentState is CustomerDetailsUiState.Success) {
            val newNote = CustomerNote(
                id = "n-${System.currentTimeMillis()}",
                customerId = customerId,
                note = noteText,
                date = "Just now",
                authorName = MockDataProvider.currentFamilyMember.name
            )
            val updatedNotes = listOf(newNote) + currentState.notes
            val updatedHistory = listOf("Note added: $noteText") + currentState.activityHistory
            
            _uiState.update { 
                currentState.copy(
                    notes = updatedNotes,
                    activityHistory = updatedHistory
                ) 
            }
        }
    }

    companion object {
        fun provideFactory(customerId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CustomerDetailsViewModel(customerId)
            }
        }
    }
}
