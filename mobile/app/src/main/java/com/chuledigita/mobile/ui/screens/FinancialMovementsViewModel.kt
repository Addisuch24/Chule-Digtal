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
        val selectedTypeFilter: MovementType? = null
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

    private fun updateFilteredList(query: String, typeFilter: MovementType?) {
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
            selectedTypeFilter = typeFilter
        )
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success
        val currentType = currentState?.selectedTypeFilter
        updateFilteredList(query, currentType)
    }

    fun onTypeFilterChanged(type: MovementType?) {
        val currentState = _uiState.value as? FinancialMovementsUiState.Success
        val currentQuery = currentState?.searchQuery ?: ""
        updateFilteredList(currentQuery, type)
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
