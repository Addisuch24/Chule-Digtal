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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FinancialMovementsUiState {
    data object Loading : FinancialMovementsUiState
    data class Success(
        val movements: List<FinancialMovement>,
        val filteredMovements: List<FinancialMovement>,
        val searchQuery: String = "",
        val selectedTypeFilter: MovementType? = null,
        val selectedMovementIds: Set<String> = emptySet(),
        val isDeletingBatch: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : FinancialMovementsUiState
    data class Error(val message: String) : FinancialMovementsUiState
}

class FinancialMovementsViewModel(
    private val financialRepository: FinancialRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<FinancialMovementsUiState>(FinancialMovementsUiState.Loading)
    val uiState: StateFlow<FinancialMovementsUiState> = _uiState.asStateFlow()

    private var allMovements: List<FinancialMovement> = emptyList()

    init {
        loadMovements()
    }

    fun loadMovements() {
        scope.launch {
            _uiState.value = FinancialMovementsUiState.Loading
            val result = financialRepository.getAllMovements()
            result.onSuccess { list ->
                allMovements = list
                updateFilteredList("", null)
            }.onFailure { error ->
                _uiState.value = FinancialMovementsUiState.Error(
                    error.message ?: "Failed to load financial movements"
                )
            }
        }
    }

    private fun updateFilteredList(
        query: String,
        typeFilter: MovementType?,
        selectedIds: Set<String> = emptySet(),
        isDeleting: Boolean = false,
        actionMessage: String? = null,
        actionError: String? = null
    ) {
        val filtered = allMovements.filter { m ->
            val matchesSearch = query.isBlank() ||
                    m.customerName.contains(query, ignoreCase = true) ||
                    m.description.contains(query, ignoreCase = true)
            val matchesType = typeFilter == null || m.type == typeFilter
            matchesSearch && matchesType
        }
        _uiState.value = FinancialMovementsUiState.Success(
            movements = allMovements,
            filteredMovements = filtered,
            searchQuery = query,
            selectedTypeFilter = typeFilter,
            selectedMovementIds = selectedIds,
            isDeletingBatch = isDeleting,
            actionMessage = actionMessage,
            actionError = actionError
        )
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success
        val currentType = currentState?.selectedTypeFilter
        val currentSelected = currentState?.selectedMovementIds ?: emptySet()
        updateFilteredList(query, currentType, currentSelected)
    }

    fun onTypeFilterChanged(type: MovementType?) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success
        val currentQuery = currentState?.searchQuery ?: ""
        val currentSelected = currentState?.selectedMovementIds ?: emptySet()
        updateFilteredList(currentQuery, type, currentSelected)
    }

    fun toggleSelectMovement(movementId: String) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success ?: return
        // Safeguard: verified movements cannot be selected for deletion
        val movement = allMovements.find { it.id == movementId }
        if (movement?.verificationStatus == VerificationStatus.VERIFIED) {
            _uiState.value = currentState.copy(
                actionError = "የተረጋገጡ የፋይናንስ እንቅስቃሴዎችን መሰረዝ አይቻልም (Verified records cannot be deleted to preserve accounting integrity)."
            )
            return
        }

        val updated = if (currentState.selectedMovementIds.contains(movementId)) {
            currentState.selectedMovementIds - movementId
        } else {
            currentState.selectedMovementIds + movementId
        }
        _uiState.value = currentState.copy(selectedMovementIds = updated)
    }

    fun selectAllMovements(selectableIds: List<String>) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success ?: return
        // Filter out any verified movement IDs
        val validIds = selectableIds.filter { id ->
            val m = allMovements.find { it.id == id }
            m?.verificationStatus != VerificationStatus.VERIFIED
        }
        _uiState.value = currentState.copy(selectedMovementIds = validIds.toSet())
    }

    fun clearMovementSelection() {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success ?: return
        _uiState.value = currentState.copy(selectedMovementIds = emptySet())
    }

    fun clearActionMessages() {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success ?: return
        _uiState.value = currentState.copy(actionMessage = null, actionError = null)
    }

    fun bulkDeleteSelectedMovements(onComplete: ((Int, Int, String) -> Unit)? = null) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success ?: return
        val selected = currentState.selectedMovementIds.toList()
        if (selected.isEmpty()) return

        _uiState.value = currentState.copy(isDeletingBatch = true, actionMessage = null, actionError = null)
        scope.launch {
            val result = financialRepository.bulkDeleteMovements(selected)
            result.onSuccess { data ->
                val deletedCount = data.deletedCount ?: data.deletedIds?.size ?: selected.size
                val skippedCount = data.skippedCount ?: data.skipped?.size ?: 0
                val deletedIdSet = data.deletedIds?.toSet() ?: selected.toSet()

                allMovements = allMovements.filterNot { it.id in deletedIdSet }
                val msg = if (skippedCount > 0) {
                    "$deletedCount እንቅስቃሴዎች በተሳካ ሁኔታ ተሰርዘዋል። $skippedCount የተረጋገጡ እንቅስቃሴዎች ለሂሳብ ኦዲት ደህንነት ሲባል ተጠብቀዋል።"
                } else {
                    "$deletedCount የፋይናንስ እንቅስቃሴዎች በተሳካ ሁኔታ ተሰርዘዋል።"
                }

                val curr = _uiState.value as? FinancialMovementsUiState.Success
                if (curr != null) {
                    updateFilteredList(
                        query = curr.searchQuery,
                        typeFilter = curr.selectedTypeFilter,
                        selectedIds = emptySet(),
                        isDeleting = false,
                        actionMessage = msg
                    )
                }
                onComplete?.invoke(deletedCount, skippedCount, msg)
            }.onFailure { err ->
                val curr = _uiState.value as? FinancialMovementsUiState.Success
                if (curr != null) {
                    _uiState.value = curr.copy(
                        isDeletingBatch = false,
                        actionError = err.message ?: "Failed to delete financial movements"
                    )
                }
                onComplete?.invoke(0, 0, err.message ?: "Failed to delete financial movements")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                FinancialMovementsViewModel(
                    financialRepository = application.container.financialRepository
                )
            }
        }
    }
}
