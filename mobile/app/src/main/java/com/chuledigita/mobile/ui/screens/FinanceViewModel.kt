package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerBalance(
    val customerId: String,
    val customerName: String,
    val customerPhone: String?,
    val balance: Double,
    val movements: List<FinancialMovement>
)

sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data class Success(
        val customersWhoOwe: List<CustomerBalance>,
        val customersWithCredit: List<CustomerBalance>,
        val totalMovementCount: Int,
        val searchQuery: String = "",
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val isSaving: Boolean = false
    ) : FinanceUiState
    data class Error(val message: String) : FinanceUiState
}

class FinanceViewModel(
    private val financialRepository: FinancialRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FinanceUiState>(FinanceUiState.Loading)
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private var allMovements: List<FinancialMovement> = emptyList()
    private var allCustomers: List<Customer> = emptyList()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = FinanceUiState.Loading

            val custResult = customerRepository.getCustomers()
            if (custResult.isSuccess) {
                allCustomers = custResult.getOrNull() ?: emptyList()
            }

            val movResult = financialRepository.getAllMovements()
            if (movResult.isSuccess) {
                allMovements = movResult.getOrNull() ?: emptyList()
            } else {
                _uiState.value = FinanceUiState.Error(
                    movResult.exceptionOrNull()?.message ?: "የፋይናንስ መረጃን መጫን አልተቻለም።"
                )
                return@launch
            }

            updateCalculatedState()
        }
    }

    private fun updateCalculatedState(query: String = "") {
        val currentState = _uiState.value
        val successMsg = (currentState as? FinanceUiState.Success)?.successMessage
        
        val validMovements = allMovements.filter { it.verificationStatus != VerificationStatus.REJECTED }
        
        // Group all balance records under one customer name as ONLY ONE customer entry
        val customerGroups = mutableMapOf<String, MutableList<FinancialMovement>>()
        validMovements.forEach { movement ->
            val matchedCust = allCustomers.find { 
                it.id == movement.customerId || 
                it.name.trim().equals(movement.customerName.trim(), ignoreCase = true) 
            }
            // Unique key by customer identity (normalized name) so all records under the same customer are counted as one
            val groupKey = matchedCust?.name?.trim()?.lowercase() 
                ?: movement.customerName.trim().lowercase().ifBlank { "customer_${movement.customerId}" }
            
            customerGroups.getOrPut(groupKey) { mutableListOf() }.add(movement)
        }

        val calculatedBalances = customerGroups.map { (_, movements) ->
            val firstMov = movements.firstOrNull()
            val matchedCust = allCustomers.find { cust ->
                movements.any { m -> cust.id == m.customerId || cust.name.trim().equals(m.customerName.trim(), ignoreCase = true) }
            }
            val customerId = matchedCust?.id ?: firstMov?.customerId ?: ""
            val name = matchedCust?.name ?: firstMov?.customerName ?: "Unknown"
            val phone = matchedCust?.phone

            val balance = movements.sumOf { 
                if (it.type == MovementType.CREDIT) it.amount else -it.amount 
            }

            CustomerBalance(
                customerId = customerId,
                customerName = name,
                customerPhone = phone,
                balance = balance,
                movements = movements.sortedByDescending { it.date }
            )
        }

        val filteredBalances = if (query.isBlank()) {
            calculatedBalances
        } else {
            calculatedBalances.filter { cb ->
                cb.customerName.contains(query, ignoreCase = true) ||
                (cb.customerPhone?.contains(query) ?: false) ||
                cb.movements.any { m -> 
                    m.description.contains(query, ignoreCase = true) ||
                    m.type.name.contains(query, ignoreCase = true)
                }
            }
        }

        val whoOwe = filteredBalances.filter { it.balance < 0 }.sortedBy { it.balance }
        val withCredit = filteredBalances.filter { it.balance > 0 }.sortedByDescending { it.balance }

        _uiState.value = FinanceUiState.Success(
            customersWhoOwe = whoOwe,
            customersWithCredit = withCredit,
            totalMovementCount = allMovements.size,
            searchQuery = query,
            successMessage = successMsg
        )
    }

    fun onSearchQueryChanged(query: String) {
        updateCalculatedState(query)
    }

    fun recordEntry(name: String, phone: String?, amount: Double, description: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is FinanceUiState.Success) {
                _uiState.value = currentState.copy(isSaving = true)
            }

            val type = if (amount < 0) MovementType.DEBIT else MovementType.CREDIT
            val absAmount = kotlin.math.abs(amount)

            val normalizedName = name.trim().replace(Regex("\\s+"), " ")
            val normalizedPhone = phone?.filter { it.isDigit() }

            var targetCustomer = allCustomers.find { 
                (normalizedPhone != null && normalizedPhone.length >= 7 && it.phone?.filter { p -> p.isDigit() } == normalizedPhone) ||
                it.name.trim().equals(normalizedName, ignoreCase = true)
            }

            if (targetCustomer == null) {
                val createResult = customerRepository.createCustomer(normalizedName, phone?.trim()?.ifBlank { null })
                if (createResult.isSuccess) {
                    targetCustomer = createResult.getOrNull()
                    if (targetCustomer != null) {
                        allCustomers = listOf(targetCustomer) + allCustomers.filterNot { 
                            it.id == targetCustomer.id || it.name.trim().equals(targetCustomer.name.trim(), ignoreCase = true)
                        }
                    }
                }
            } else {
                if (!phone.isNullOrBlank() && targetCustomer.phone != phone.trim()) {
                    customerRepository.updateCustomer(targetCustomer.id, targetCustomer.name, phone.trim())
                    val updated = targetCustomer.copy(phone = phone.trim())
                    targetCustomer = updated
                    allCustomers = allCustomers.map { if (it.id == updated.id) updated else it }
                }
            }

            if (targetCustomer != null) {
                val movResult = financialRepository.createMovement(
                    customerId = targetCustomer.id,
                    amount = absAmount,
                    type = type,
                    description = description.ifBlank { "የፋይናንስ እንቅስቃሴ" }
                )

                if (movResult.isSuccess) {
                    val newMov = movResult.getOrNull()
                    if (newMov != null) {
                        allMovements = listOf(newMov) + allMovements
                    }
                    val updatedState = _uiState.value
                    if (updatedState is FinanceUiState.Success) {
                        _uiState.value = updatedState.copy(
                            successMessage = "መረጃው በተሳካ ሁኔታ ተመዝግቧል።",
                            isSaving = false
                        )
                    }
                    updateCalculatedState((_uiState.value as? FinanceUiState.Success)?.searchQuery ?: "")
                    return@launch
                }
            }
            
            val updatedState = _uiState.value
            if (updatedState is FinanceUiState.Success) {
                _uiState.value = updatedState.copy(
                    errorMessage = "መረጃውን መመዝገብ አልተቻለም።",
                    isSaving = false
                )
            }
        }
    }

    fun deleteMovement(movementId: String) {
        viewModelScope.launch {
            val result = financialRepository.deleteMovement(movementId)
            if (result.isSuccess) {
                allMovements = allMovements.filter { it.id != movementId }
                updateCalculatedState((_uiState.value as? FinanceUiState.Success)?.searchQuery ?: "")
            }
        }
    }

    fun deleteCustomer(customerId: String, customerName: String) {
        viewModelScope.launch {
            val result = customerRepository.deleteCustomer(customerId)
            if (result.isSuccess) {
                allCustomers = allCustomers.filterNot { 
                    it.id == customerId || it.name.trim().equals(customerName.trim(), ignoreCase = true) 
                }
                allMovements = allMovements.filterNot { 
                    it.customerId == customerId || it.customerName.trim().equals(customerName.trim(), ignoreCase = true) 
                }
                val updatedState = _uiState.value
                if (updatedState is FinanceUiState.Success) {
                    _uiState.value = updatedState.copy(successMessage = "ደንበኛው በተሳካ ሁኔታ ተሰርዟል።")
                }
                updateCalculatedState((_uiState.value as? FinanceUiState.Success)?.searchQuery ?: "")
            } else {
                val updatedState = _uiState.value
                if (updatedState is FinanceUiState.Success) {
                    _uiState.value = updatedState.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "ደንበኛውን መሰረዝ አልተቻለም።"
                    )
                }
            }
        }
    }

    fun updateMovement(movementId: String, customerId: String, amount: Double, description: String) {
        viewModelScope.launch {
            val type = if (amount < 0) MovementType.DEBIT else MovementType.CREDIT
            val absAmount = kotlin.math.abs(amount)

            val result = financialRepository.updateMovement(movementId, customerId, absAmount, type, description)
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null) {
                    allMovements = allMovements.map { m ->
                        if (m.id == movementId) updated else m
                    }
                    updateCalculatedState((_uiState.value as? FinanceUiState.Success)?.searchQuery ?: "")
                }
            }
        }
    }

    fun clearMessages() {
        val currentState = _uiState.value
        if (currentState is FinanceUiState.Success) {
            _uiState.value = currentState.copy(successMessage = null, errorMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                FinanceViewModel(
                    financialRepository = application.container.financialRepository,
                    customerRepository = application.container.customerRepository
                )
            }
        }
    }
}
