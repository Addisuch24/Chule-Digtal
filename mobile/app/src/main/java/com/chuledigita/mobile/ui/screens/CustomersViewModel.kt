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
                        event.type == NotificationType.CUSTOMER_UPDATED ||
                        event.type == NotificationType.CUSTOMER_DELETED ||
                        event.type == NotificationType.CUSTOMER_BULK_DELETED ||
                        event.type == NotificationType.CUSTOMER_NOTE_CREATED) {
                        loadCustomers()
                    }
                }
            }
        }
    }

    fun loadCustomers(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading && _uiState.value !is CustomersUiState.Success) {
                _uiState.value = CustomersUiState.Loading
            }
            val result = customerRepository.getCustomers()
            result.fold(
                onSuccess = { list ->
                    // Group records under the same customer name so each customer name is only counted as one
                    val distinctCustomers = list.groupBy { it.name.trim().lowercase() }.map { (_, group) ->
                        group.first()
                    }
                    if (distinctCustomers.isEmpty()) {
                        _uiState.value = CustomersUiState.Empty
                    } else {
                        val current = _uiState.value
                        if (current is CustomersUiState.Success) {
                            _uiState.value = current.copy(customers = distinctCustomers)
                        } else {
                            _uiState.value = CustomersUiState.Success(customers = distinctCustomers)
                        }
                    }
                },
                onFailure = { error ->
                    val current = _uiState.value
                    if (current is CustomersUiState.Success) {
                        _uiState.value = current.copy(actionError = error.message)
                    } else {
                        _uiState.value = CustomersUiState.Error(
                            error.message ?: "Failed to fetch customers from server."
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        loadCustomers(showLoading = true)
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

    fun toggleSelectCustomer(customerId: String) {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        val updated = if (currentState.selectedIds.contains(customerId)) {
            currentState.selectedIds - customerId
        } else {
            currentState.selectedIds + customerId
        }
        _uiState.value = currentState.copy(selectedIds = updated)
    }

    fun selectAllCustomers(visibleCustomerIds: List<String>) {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        val allSelected = visibleCustomerIds.isNotEmpty() && currentState.selectedIds.containsAll(visibleCustomerIds)
        val updated = if (allSelected) {
            emptySet()
        } else {
            visibleCustomerIds.toSet()
        }
        _uiState.value = currentState.copy(selectedIds = updated)
    }

    fun clearCustomerSelection() {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        _uiState.value = currentState.copy(selectedIds = emptySet())
    }

    fun deleteCustomer(customerId: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isDeleting = true, actionMessage = null, actionError = null)
            val result = customerRepository.deleteCustomer(customerId)
            result.fold(
                onSuccess = {
                    val remaining = currentState.customers.filterNot { it.id == customerId }
                    val message = "ደንበኛው በተሳካ ሁኔታ ተሰርዟል።"
                    if (remaining.isEmpty()) {
                        _uiState.value = CustomersUiState.Empty
                    } else {
                        _uiState.value = currentState.copy(
                            customers = remaining,
                            selectedIds = currentState.selectedIds - customerId,
                            isDeleting = false,
                            actionMessage = message,
                            actionError = null
                        )
                    }
                    loadCustomers(showLoading = false)
                    onComplete?.invoke(true, message)
                },
                onFailure = { error ->
                    val current = _uiState.value as? CustomersUiState.Success ?: currentState
                    val errMsg = error.message ?: "ደንበኛውን መሰረዝ አልተቻለም።"
                    _uiState.value = current.copy(
                        isDeleting = false,
                        actionError = errMsg,
                        actionMessage = null
                    )
                    onComplete?.invoke(false, errMsg)
                }
            )
        }
    }

    fun bulkDeleteSelectedCustomers(onComplete: ((deletedCount: Int, skippedCount: Int, message: String) -> Unit)? = null) {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        val idsToDelete = currentState.selectedIds.toList()
        if (idsToDelete.isEmpty() || currentState.isDeleting) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isDeleting = true, actionMessage = null, actionError = null)
            val result = customerRepository.bulkDeleteCustomers(idsToDelete)
            result.fold(
                onSuccess = { data ->
                    val skippedCount = data.skipped?.size ?: 0
                    if (data.deletedCount == 0) {
                        val reason = data.skipped?.firstOrNull()?.reason
                            ?: "ደንበኞችን መሰረዝ አልተቻለም። ከፋይናንስ እንቅስቃሴ ጋር የተያያዙ መረጃዎች ተጠብቀው መቆየት አለባቸው።"
                        val current = _uiState.value as? CustomersUiState.Success
                        if (current != null) {
                            _uiState.value = current.copy(
                                selectedIds = emptySet(),
                                isDeleting = false,
                                actionError = reason,
                                actionMessage = null
                            )
                        }
                        onComplete?.invoke(0, skippedCount, reason)
                        return@fold
                    }

                    val deletedIdSet = data.deletedIds.map { it.toString() }.toSet().ifEmpty { idsToDelete.toSet() }
                    val current = _uiState.value as? CustomersUiState.Success ?: currentState
                    val remaining = current.customers.filterNot { it.id in deletedIdSet }

                    val message = if (skippedCount > 0) {
                        "${data.deletedCount} ደንበኞች ተሰርዘዋል። $skippedCount ደንበኞች ከፋይናንስ እንቅስቃሴ ጋር የተያያዙ በመሆናቸው ተጠብቀዋል።"
                    } else {
                        "${data.deletedCount} ደንበኞች በተሳካ ሁኔታ ተሰርዘዋል።"
                    }

                    if (remaining.isEmpty()) {
                        _uiState.value = CustomersUiState.Empty
                    } else {
                        _uiState.value = current.copy(
                            customers = remaining,
                            selectedIds = emptySet(),
                            isDeleting = false,
                            actionMessage = message,
                            actionError = null
                        )
                    }
                    loadCustomers(showLoading = false)
                    onComplete?.invoke(data.deletedCount, skippedCount, message)
                },
                onFailure = { error ->
                    val current = _uiState.value as? CustomersUiState.Success ?: currentState
                    val errMsg = error.message ?: "ደንበኞችን መሰረዝ አልተቻለም።"
                    _uiState.value = current.copy(
                        selectedIds = emptySet(),
                        isDeleting = false,
                        actionError = errMsg,
                        actionMessage = null
                    )
                    onComplete?.invoke(0, 0, errMsg)
                }
            )
        }
    }

    fun clearActionMessages() {
        val currentState = _uiState.value as? CustomersUiState.Success ?: return
        _uiState.value = currentState.copy(actionMessage = null, actionError = null)
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

    fun updateCustomer(
        customerId: String,
        name: String,
        phone: String?,
        note: String?,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            val result = customerRepository.updateCustomer(customerId = customerId, name = name, phone = phone)
            result.fold(
                onSuccess = { updatedCustomer ->
                    if (!note.isNullOrBlank()) {
                        customerRepository.createCustomerNote(updatedCustomer.id, note.trim())
                    }
                    refresh()
                    onComplete(true, "ደንበኛ በተሳካ ሁኔታ ተሻሽሏል")
                },
                onFailure = { error ->
                    onComplete(false, error.message ?: "Failed to update customer")
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
