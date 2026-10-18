package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
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
    private val _financialRepository: FinancialRepository,
    private val _customerRepository: CustomerRepository,
    private val _notificationRepository: NotificationRepository? = null,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<FinanceUiState>(FinanceUiState.Loading)
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private var allMovements: List<FinancialMovement> = emptyList()
    private var allCustomers: List<Customer> = emptyList()

    init {
        loadData()

        if (_notificationRepository != null) {
            scope.launch {
                _notificationRepository.realtimeEventFlow.collect { event ->
                    if (event.type == NotificationType.FINANCIAL_MOVEMENT_CREATED ||
                        event.type == NotificationType.FINANCIAL_MOVEMENT_VERIFIED ||
                        event.type == NotificationType.FINANCIAL_MOVEMENT_REJECTED ||
                        event.type == NotificationType.PAYMENT_RECORDED) {
                        loadData()
                    }
                }
            }
        }
    }

    fun loadData() {
        scope.launch {
            _uiState.value = FinanceUiState.Loading

            val custResult = _customerRepository.getCustomers()
            val movResult = _financialRepository.getAllMovements()

            if (custResult.isSuccess && movResult.isSuccess) {
                allCustomers = custResult.getOrNull() ?: emptyList()
                allMovements = movResult.getOrNull() ?: emptyList()
                updateCalculatedState()
            } else {
                val errorMsg = movResult.exceptionOrNull()?.message
                    ?: custResult.exceptionOrNull()?.message
                    ?: "የፋይናንስ መረጃዎችን ማምጣት አልተቻለም።"
                _uiState.value = FinanceUiState.Error(errorMsg)
            }
        }
    }

    private fun updateCalculatedState(query: String = "") {
        val currentState = _uiState.value
        val successMsg = (currentState as? FinanceUiState.Success)?.successMessage
        val errorMsg = (currentState as? FinanceUiState.Success)?.errorMessage

        val validMovements = allMovements.filter { it.verificationStatus != VerificationStatus.REJECTED }

        val customerBalancesMap = mutableMapOf<String, MutableList<FinancialMovement>>()
        validMovements.forEach { movement ->
            customerBalancesMap.getOrPut(movement.customerId) { mutableListOf() }.add(movement)
        }

        val calculatedBalances = customerBalancesMap.map { (customerId, movements) ->
            val customer = allCustomers.find { it.id == customerId }
            val name = customer?.name ?: movements.firstOrNull()?.customerName ?: "ደንበኛ #$customerId"
            val phone = customer?.phone

            // Formula: balance = total_credit - total_debit
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
            val q = query.trim().lowercase()
            calculatedBalances.filter { cb ->
                cb.customerName.lowercase().contains(q) ||
                (cb.customerPhone?.contains(q) == true) ||
                cb.movements.any { m ->
                    m.description.lowercase().contains(q) ||
                    m.type.name.lowercase().contains(q)
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
            successMessage = successMsg,
            errorMessage = errorMsg,
            isSaving = false
        )
    }

    fun onSearchQueryChanged(query: String) {
        updateCalculatedState(query)
    }

    fun recordEntry(name: String, phone: String?, amount: Double, description: String) {
        val trimmedName = name.trim()
        val trimmedDesc = description.trim()

        if (trimmedName.isBlank()) {
            setErrorMessage("እባክዎ የደንበኛ ስም ያስገቡ።")
            return
        }
        if (amount == 0.0) {
            setErrorMessage("እባክዎ ከዜሮ የተለየ የገንዘብ መጠን ያስገቡ።")
            return
        }

        scope.launch {
            setSaving(true)

            // Look up or create customer
            var targetCustomer = allCustomers.find {
                it.name.equals(trimmedName, ignoreCase = true) ||
                (!phone.isNullOrBlank() && it.phone == phone.trim())
            }

            if (targetCustomer == null) {
                val createCustResult = _customerRepository.createCustomer(
                    name = trimmedName,
                    phone = phone?.trim()?.ifBlank { null }
                )
                if (createCustResult.isSuccess) {
                    targetCustomer = createCustResult.getOrNull()
                } else {
                    setErrorMessage(
                        createCustResult.exceptionOrNull()?.message
                            ?: "ደንበኛ መመዝገብ አልተቻለም።"
                    )
                    return@launch
                }
            }

            if (targetCustomer == null) {
                setErrorMessage("ደንበኛ ማግኘት አልተቻለም።")
                return@launch
            }

            val movementType = if (amount < 0) MovementType.DEBIT else MovementType.CREDIT
            val absAmount = kotlin.math.abs(amount)

            val createMovResult = _financialRepository.createMovement(
                customerId = targetCustomer.id,
                amount = absAmount,
                type = movementType,
                description = trimmedDesc.ifBlank { "የፋይናንስ እንቅስቃሴ" }
            )

            createMovResult.onSuccess {
                // Refresh data from backend
                val refreshCust = _customerRepository.getCustomers()
                val refreshMov = _financialRepository.getAllMovements()
                if (refreshCust.isSuccess && refreshMov.isSuccess) {
                    allCustomers = refreshCust.getOrNull() ?: allCustomers
                    allMovements = refreshMov.getOrNull() ?: allMovements
                }
                updateCalculatedState()
                val current = _uiState.value as? FinanceUiState.Success
                if (current != null) {
                    _uiState.value = current.copy(
                        successMessage = "መረጃው በተሳካ ሁኔታ ተመዝግቧል።",
                        errorMessage = null,
                        isSaving = false
                    )
                }
            }.onFailure { error ->
                setErrorMessage(error.message ?: "የፋይናንስ እንቅስቃሴ መመዝገብ አልተቻለም።")
            }
        }
    }

    fun deleteMovement(movementId: String) {
        scope.launch {
            setSaving(true)
            val result = _financialRepository.deleteMovement(movementId)
            result.onSuccess {
                val refreshMov = _financialRepository.getAllMovements()
                if (refreshMov.isSuccess) {
                    allMovements = refreshMov.getOrNull() ?: allMovements
                }
                updateCalculatedState()
                val current = _uiState.value as? FinanceUiState.Success
                if (current != null) {
                    _uiState.value = current.copy(
                        successMessage = "የፋይናንስ እንቅስቃሴው ተሰርዟል።",
                        errorMessage = null,
                        isSaving = false
                    )
                }
            }.onFailure { error ->
                setErrorMessage(error.message ?: "የፋይናንስ እንቅስቃሴ መቆረጥ አልተቻለም።")
            }
        }
    }

    fun updateMovement(movementId: String, customerId: String, amount: Double, description: String) {
        scope.launch {
            setSaving(true)
            val movementType = if (amount < 0) MovementType.DEBIT else MovementType.CREDIT
            val absAmount = kotlin.math.abs(amount)

            val result = _financialRepository.updateMovement(
                movementId = movementId,
                customerId = customerId,
                amount = absAmount,
                type = movementType,
                description = description.trim()
            )

            result.onSuccess {
                val refreshMov = _financialRepository.getAllMovements()
                if (refreshMov.isSuccess) {
                    allMovements = refreshMov.getOrNull() ?: allMovements
                }
                updateCalculatedState()
                val current = _uiState.value as? FinanceUiState.Success
                if (current != null) {
                    _uiState.value = current.copy(
                        successMessage = "የፋይናንስ እንቅስቃሴው ተስተካክሏል።",
                        errorMessage = null,
                        isSaving = false
                    )
                }
            }.onFailure { error ->
                setErrorMessage(error.message ?: "የፋይናንስ እንቅስቃሴ ማስተካከል አልተቻለም።")
            }
        }
    }

    fun clearMessages() {
        val currentState = _uiState.value as? FinanceUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(successMessage = null, errorMessage = null)
        }
    }

    private fun setSaving(saving: Boolean) {
        val current = _uiState.value as? FinanceUiState.Success
        if (current != null) {
            _uiState.value = current.copy(isSaving = saving)
        }
    }

    private fun setErrorMessage(msg: String) {
        val current = _uiState.value as? FinanceUiState.Success
        if (current != null) {
            _uiState.value = current.copy(errorMessage = msg, isSaving = false)
        } else {
            _uiState.value = FinanceUiState.Error(msg)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                FinanceViewModel(
                    _financialRepository = application.container.financialRepository,
                    _customerRepository = application.container.customerRepository,
                    _notificationRepository = application.container.notificationRepository
                )
            }
        }
    }
}
