package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.repository.FinancialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FinancialMovementDetailsUiState {
    data object Loading : FinancialMovementDetailsUiState
    data class Success(
        val movement: FinancialMovement,
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val isSubmitting: Boolean = false
    ) : FinancialMovementDetailsUiState
    data class Error(val message: String) : FinancialMovementDetailsUiState
}

class FinancialMovementDetailsViewModel(
    private val financialRepository: FinancialRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<FinancialMovementDetailsUiState>(FinancialMovementDetailsUiState.Loading)
    val uiState: StateFlow<FinancialMovementDetailsUiState> = _uiState.asStateFlow()

    fun loadMovement(movementId: String) {
        scope.launch {
            _uiState.value = FinancialMovementDetailsUiState.Loading
            val result = financialRepository.getMovementById(movementId)
            result.onSuccess { movement ->
                _uiState.value = FinancialMovementDetailsUiState.Success(movement)
            }.onFailure { error ->
                _uiState.value = FinancialMovementDetailsUiState.Error(
                    error.message ?: "Transaction not found."
                )
            }
        }
    }

    fun verifyMovement(movementId: String, note: String) {
        val currentState = _uiState.value as? FinancialMovementDetailsUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        if (note.isBlank()) {
            if (currentState != null) {
                _uiState.value = currentState.copy(errorMessage = "Verification note is required.")
            }
            return
        }

        scope.launch {
            if (currentState != null) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }
            val result = financialRepository.verifyMovement(movementId, note.trim())
            result.onSuccess { updated ->
                _uiState.value = FinancialMovementDetailsUiState.Success(
                    movement = updated,
                    successMessage = "Transaction verified successfully.",
                    isSubmitting = false
                )
            }.onFailure { error ->
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        errorMessage = error.message ?: "Verification failed.",
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun rejectMovement(movementId: String, note: String) {
        val currentState = _uiState.value as? FinancialMovementDetailsUiState.Success
        if (currentState != null && currentState.isSubmitting) return

        if (note.isBlank()) {
            if (currentState != null) {
                _uiState.value = currentState.copy(errorMessage = "Rejection note is required.")
            }
            return
        }

        scope.launch {
            if (currentState != null) {
                _uiState.value = currentState.copy(isSubmitting = true)
            }
            val result = financialRepository.rejectMovement(movementId, note.trim())
            result.onSuccess { updated ->
                _uiState.value = FinancialMovementDetailsUiState.Success(
                    movement = updated,
                    successMessage = "Transaction rejected.",
                    isSubmitting = false
                )
            }.onFailure { error ->
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        errorMessage = error.message ?: "Rejection failed.",
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun clearMessages() {
        val currentState = _uiState.value as? FinancialMovementDetailsUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(successMessage = null, errorMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                FinancialMovementDetailsViewModel(
                    financialRepository = application.container.financialRepository
                )
            }
        }
    }
}
