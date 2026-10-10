package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.CustomerNote

/**
 * UI state for the global Business Notes screen.
 */
sealed interface NotesUiState {
    data object Loading : NotesUiState
    
    data class Success(
        val notes: List<CustomerNote>,
        val isCreating: Boolean = false,
        val successMessage: String? = null
    ) : NotesUiState

    data object Empty : NotesUiState
    
    data class Error(val message: String) : NotesUiState
}
