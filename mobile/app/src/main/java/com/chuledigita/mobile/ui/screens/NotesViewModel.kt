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
 * ViewModel managing the global shared family-business notes with local sample data.
 */
class NotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    // Local in-memory store for sample data
    private val localNotesList = mutableListOf(
        CustomerNote(
            id = "1",
            customerId = "101",
            note = "ለነገ ጧት 5 ካርቶን ወተት ለሱቅ 10 እንዲረከብ ማሳሰቢያ ተሰጥቷል።",
            date = "2023-11-15",
            time = "09:30 AM",
            authorName = "ዮሐንስ አበበ"
        ),
        CustomerNote(
            id = "2",
            customerId = "102",
            note = "የአቶ ከበደ የባንክ ማረጋገጫ ደረሰኝ በንግድ ባንክ በኩል ደርሶናል። እባክችሁ Verification Queue ላይ አረጋግጡ።",
            date = "2023-11-15",
            time = "10:15 AM",
            authorName = "አልማዝ ተሰማ"
        ),
        CustomerNote(
            id = "3",
            customerId = "103",
            note = "አዲስ ደንበኛ (ኪያ ሱፐርማርኬት) ተመዝግቧል። የመጀመሪያ ትዕዛዝ እንዲላክላቸው ይፈልጋሉ።",
            date = "2023-11-14",
            time = "04:45 PM",
            authorName = "ብሩክ ታደሰ"
        )
    )

    init {
        loadNotes()
    }

    /**
     * Loads the local sample data without connecting to the real network database.
     */
    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = NotesUiState.Loading
            if (localNotesList.isEmpty()) {
                _uiState.value = NotesUiState.Empty
            } else {
                _uiState.value = NotesUiState.Success(notes = localNotesList.toList())
            }
        }
    }

    /**
     * Creates a new shared business note locally in the in-memory sample collection.
     */
    fun addNote(content: String) {
        val currentState = _uiState.value
        if (currentState is NotesUiState.Success || currentState is NotesUiState.Empty) {
            viewModelScope.launch {
                val newNote = CustomerNote(
                    id = UUID.randomUUID().toString(),
                    customerId = "0",
                    note = content,
                    date = "ዛሬ",
                    time = "አሁን",
                    authorName = "የአሁኑ አባል"
                )
                localNotesList.add(0, newNote)
                
                _uiState.value = NotesUiState.Success(
                    notes = localNotesList.toList(),
                    successMessage = "ማስታወሻው በተሳካ ሁኔታ ተመዝግቧል።"
                )
            }
        }
    }

    fun clearSuccessMessage() {
        val currentState = _uiState.value
        if (currentState is NotesUiState.Success) {
            _uiState.value = currentState.copy(successMessage = null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                NotesViewModel(noteRepository = application.container.noteRepository)
            }
        }
    }
}
