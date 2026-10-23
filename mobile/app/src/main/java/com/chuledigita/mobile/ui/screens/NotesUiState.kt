package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.CustomerNote

/**
 * UI state for the global Business Notes screen matching the web notes workspace.
 */
sealed interface NotesUiState {
    data object Loading : NotesUiState
    
    data class Success(
        val notes: List<CustomerNote>,
        val filteredNotes: List<CustomerNote>,
        val searchQuery: String = "",
        val editingNote: CustomerNote? = null,
        val selectedNote: CustomerNote? = null,
        val selectedNoteIds: Set<String> = emptySet(),
        val isSubmitting: Boolean = false,
        val isDeletingBatch: Boolean = false,
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val batchDeleteMessage: String? = null,
        val batchDeleteError: String? = null
    ) : NotesUiState

    data object Empty : NotesUiState
    
    data class Error(val message: String) : NotesUiState
}
