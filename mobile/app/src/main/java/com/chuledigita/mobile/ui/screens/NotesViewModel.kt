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

/**
 * ViewModel managing the global shared family-business notes connected to real backend API.
 */
class NotesViewModel(
    private val _noteRepository: NoteRepository,
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var allNotes: List<CustomerNote> = emptyList()
    private var currentSearchQuery: String = ""

    init {
        loadNotes()
    }

    /**
     * Loads global notes from backend API repository.
     */
    fun loadNotes() {
        scope.launch {
            _uiState.value = NotesUiState.Loading
            val result = _noteRepository.getGlobalNotes()
            if (result.isSuccess) {
                allNotes = result.getOrNull() ?: emptyList()
                updateState()
            } else {
                _uiState.value = NotesUiState.Error(
                    result.exceptionOrNull()?.message ?: "ማስታወሻዎችን መጫን አልተቻለም።"
                )
            }
        }
    }

    private fun updateState(
        successMsg: String? = null,
        errorMsg: String? = null,
        editing: CustomerNote? = null,
        selected: CustomerNote? = null,
        submitting: Boolean = false,
        selectedIds: Set<String>? = null,
        deletingBatch: Boolean = false,
        batchMsg: String? = null,
        batchErr: String? = null
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

        val currentSuccess = _uiState.value as? NotesUiState.Success
        val currentIds = selectedIds ?: currentSuccess?.selectedNoteIds ?: emptySet()

        _uiState.value = NotesUiState.Success(
            notes = allNotes,
            filteredNotes = filtered,
            searchQuery = currentSearchQuery,
            editingNote = editing,
            selectedNote = selected,
            selectedNoteIds = currentIds,
            isSubmitting = submitting,
            isDeletingBatch = deletingBatch,
            successMessage = successMsg,
            errorMessage = errorMsg,
            batchDeleteMessage = batchMsg,
            batchDeleteError = batchErr
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
                selected = currentState.selectedNote,
                selectedIds = currentState.selectedNoteIds
            )
        } else if (currentState is NotesUiState.Empty && query.isNotBlank()) {
            updateState()
        }
    }

    /**
     * Creates a new shared note via backend API.
     */
    fun addNote(content: String) {
        if (content.isBlank()) return
        scope.launch {
            setSubmitting(true)
            val result = _noteRepository.createGlobalNote(content.trim())
            if (result.isSuccess) {
                val newNote = result.getOrNull()
                if (newNote != null) {
                    allNotes = listOf(newNote) + allNotes
                } else {
                    loadNotes()
                }
                updateState(selected = null)
            } else {
                setSubmitting(false)
                updateState(errorMsg = result.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * Updates an existing note via backend API.
     */
    fun updateNote(noteId: String, content: String) {
        if (content.isBlank()) return
        scope.launch {
            setSubmitting(true)
            val result = _noteRepository.updateGlobalNote(noteId, content.trim())
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null) {
                    allNotes = allNotes.map { if (it.id == noteId) updated else it }
                } else {
                    loadNotes()
                }
                updateState(editing = null, selected = null)
            } else {
                setSubmitting(false)
                updateState(errorMsg = result.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * Deletes a note via backend API.
     */
    fun deleteNote(noteId: String) {
        scope.launch {
            val result = _noteRepository.deleteGlobalNote(noteId)
            if (result.isSuccess) {
                allNotes = allNotes.filter { it.id != noteId }
                val currentState = _uiState.value as? NotesUiState.Success
                val nextSelected = if (currentState?.selectedNote?.id == noteId) null else currentState?.selectedNote
                val nextEditing = if (currentState?.editingNote?.id == noteId) null else currentState?.editingNote
                updateState(
                    editing = nextEditing,
                    selected = nextSelected
                )
            } else {
                updateState(errorMsg = result.exceptionOrNull()?.message)
            }
        }
    }

    fun startEditing(note: CustomerNote) {
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
                selected = currentState.selectedNote,
                selectedIds = currentState.selectedNoteIds
            )
        }
    }

    fun toggleSelectNote(noteId: String) {
        val currentState = _uiState.value as? NotesUiState.Success ?: return
        val updated = if (currentState.selectedNoteIds.contains(noteId)) {
            currentState.selectedNoteIds - noteId
        } else {
            currentState.selectedNoteIds + noteId
        }
        updateState(
            successMsg = currentState.successMessage,
            errorMsg = currentState.errorMessage,
            editing = currentState.editingNote,
            selected = currentState.selectedNote,
            submitting = currentState.isSubmitting,
            selectedIds = updated
        )
    }

    fun selectAllNotes(visibleIds: List<String>) {
        val currentState = _uiState.value as? NotesUiState.Success ?: return
        val allSelected = visibleIds.isNotEmpty() && currentState.selectedNoteIds.containsAll(visibleIds)
        val updated = if (allSelected) emptySet() else visibleIds.toSet()
        updateState(
            successMsg = currentState.successMessage,
            errorMsg = currentState.errorMessage,
            editing = currentState.editingNote,
            selected = currentState.selectedNote,
            submitting = currentState.isSubmitting,
            selectedIds = updated
        )
    }

    fun clearNoteSelection() {
        val currentState = _uiState.value as? NotesUiState.Success ?: return
        updateState(
            successMsg = currentState.successMessage,
            errorMsg = currentState.errorMessage,
            editing = currentState.editingNote,
            selected = currentState.selectedNote,
            submitting = currentState.isSubmitting,
            selectedIds = emptySet()
        )
    }

    fun bulkDeleteSelectedNotes(onComplete: ((deletedCount: Int, message: String) -> Unit)? = null) {
        val currentState = _uiState.value as? NotesUiState.Success ?: return
        val idsToDelete = currentState.selectedNoteIds.toList()
        if (idsToDelete.isEmpty() || currentState.isDeletingBatch) return

        scope.launch {
            updateState(
                editing = currentState.editingNote,
                selected = currentState.selectedNote,
                selectedIds = currentState.selectedNoteIds,
                deletingBatch = true
            )
            val result = _noteRepository.bulkDeleteGlobalNotes(idsToDelete)
            if (result.isSuccess) {
                val data = result.getOrNull()
                val deletedCount = data?.deletedCount ?: idsToDelete.size
                allNotes = allNotes.filter { it.id !in idsToDelete }
                val nextSelected = if (currentState.selectedNote?.id in idsToDelete) null else currentState.selectedNote
                val msg = "ማስታወሻዎች በተሳካ ሁኔታ ተሰርዘዋል። ($deletedCount)"
                updateState(
                    selected = nextSelected,
                    selectedIds = emptySet(),
                    deletingBatch = false,
                    batchMsg = msg
                )
                onComplete?.invoke(deletedCount, msg)
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "ማስታወሻዎችን መሰረዝ አልተቻለም።"
                updateState(
                    editing = currentState.editingNote,
                    selected = currentState.selectedNote,
                    selectedIds = currentState.selectedNoteIds,
                    deletingBatch = false,
                    batchErr = errMsg
                )
                onComplete?.invoke(0, errMsg)
            }
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
