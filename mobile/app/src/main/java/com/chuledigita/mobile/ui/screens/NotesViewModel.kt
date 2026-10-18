package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing the global shared family-business notes connected to mock/real backend API.
 */
class NotesViewModel(
    private val _noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var allNotes: List<CustomerNote> = emptyList()
    private var currentSearchQuery: String = ""

    init {
        loadNotes()
    }

    /**
     * Loads global notes (mock sample data for UI design mode).
     */
    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = NotesUiState.Loading
            kotlinx.coroutines.delay(150)

            // UI Design mode: Load sample mock note matching reference screenshot
            allNotes = listOf(
                CustomerNote(
                    id = "n1",
                    customerId = "c1",
                    note = "sdfhmnh\nghty hjty h",
                    date = "Oct 11, 2026",
                    time = "11:37 PM",
                    authorName = "Sode"
                )
            )
            updateState()
        }
    }

    private fun updateState(
        successMsg: String? = null,
        errorMsg: String? = null,
        editing: CustomerNote? = null,
        selected: CustomerNote? = null,
        submitting: Boolean = false
    ) {
        if (allNotes.isEmpty() && currentSearchQuery.isBlank()) {
            _uiState.value = NotesUiState.Empty
            return
        }

        val filtered = if (currentSearchQuery.isBlank()) {
            allNotes
        } else {
            val q = currentSearchQuery.trim().lowercase()
            allNotes.filter { note ->
                note.note.lowercase().contains(q) || note.authorName.lowercase().contains(q)
            }
        }

        _uiState.value = NotesUiState.Success(
            notes = allNotes,
            filteredNotes = filtered,
            searchQuery = currentSearchQuery,
            editingNote = editing,
            selectedNote = selected,
            isSubmitting = submitting,
            successMessage = successMsg,
            errorMessage = errorMsg
        )
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        val currentState = _uiState.value
        if (currentState is NotesUiState.Success) {
            updateState(
                successMsg = currentState.successMessage,
                errorMsg = currentState.errorMessage,
                editing = currentState.editingNote,
                selected = currentState.selectedNote
            )
        } else if (currentState is NotesUiState.Empty && query.isNotBlank()) {
            updateState()
        }
    }

    /**
     * Creates a new shared note automatically without distractions.
     */
    fun addNote(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            setSubmitting(true)
            kotlinx.coroutines.delay(100)

            val newNote = CustomerNote(
                id = UUID.randomUUID().toString(),
                customerId = "c1",
                note = content.trim(),
                date = "Oct 11, 2026",
                time = "12:00 PM",
                authorName = "አስተዳዳሪ"
            )
            allNotes = listOf(newNote) + allNotes
            updateState(
                successMsg = null, // Saved silently without distractions
                selected = null
            )
        }
    }

    /**
     * Updates an existing note automatically without distractions.
     */
    fun updateNote(noteId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            setSubmitting(true)
            kotlinx.coroutines.delay(100)

            allNotes = allNotes.map { note ->
                if (note.id == noteId) note.copy(note = content.trim()) else note
            }
            updateState(
                successMsg = null, // Saved silently without distractions
                editing = null,
                selected = null
            )
        }
    }

    /**
     * Deletes a note.
     */
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            allNotes = allNotes.filter { it.id != noteId }
            val currentState = _uiState.value as? NotesUiState.Success
            val nextSelected = if (currentState?.selectedNote?.id == noteId) null else currentState?.selectedNote
            val nextEditing = if (currentState?.editingNote?.id == noteId) null else currentState?.editingNote
            updateState(
                successMsg = null,
                editing = nextEditing,
                selected = nextSelected
            )
        }
    }

    fun startEditing(note: CustomerNote) {
        val currentState = _uiState.value as? NotesUiState.Success
        updateState(
            editing = note,
            selected = null
        )
    }

    fun cancelEditing() {
        updateState(editing = null)
    }

    fun selectNote(note: CustomerNote?) {
        val currentState = _uiState.value as? NotesUiState.Success
        updateState(
            editing = currentState?.editingNote,
            selected = note
        )
    }

    fun clearMessages() {
        val currentState = _uiState.value as? NotesUiState.Success
        if (currentState != null) {
            updateState(
                editing = currentState.editingNote,
                selected = currentState.selectedNote
            )
        }
    }

    private fun setSubmitting(submitting: Boolean) {
        val currentState = _uiState.value as? NotesUiState.Success
        if (currentState != null) {
            _uiState.value = currentState.copy(isSubmitting = submitting)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                NotesViewModel(_noteRepository = application.container.noteRepository)
            }
        }
    }
}
