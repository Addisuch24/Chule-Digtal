package com.chuledigita.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.ui.components.ConfirmDialog
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScaffold(
        navController = navController,
        title = "ማስታወሻዎች",
        showBottomBar = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreen)
                    }
                }

                is NotesUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = ErrorColor,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadNotes() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            Text("እንደገና ሞክር")
                        }
                    }
                }

                is NotesUiState.Empty -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            NotesHeader(notesCount = 0)
                        }

                        item {
                            NoteInputCard(
                                editingNote = null,
                                isSubmitting = false,
                                onSave = { text -> viewModel.addNote(text) },
                                onCancel = {}
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = null,
                                        tint = BrandGreen.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "ምንም ማስታወሻ አልተመዘገበም።",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ከላይ አዲስ ማስታወሻ መመዝገብ ይችላሉ።",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandGreen.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                is NotesUiState.Success -> {
                    NotesContent(
                        state = state,
                        onAddNote = viewModel::addNote,
                        onUpdateNote = viewModel::updateNote,
                        onDeleteNote = viewModel::deleteNote,
                        onStartEditing = viewModel::startEditing,
                        onCancelEditing = viewModel::cancelEditing,
                        onSelectNote = viewModel::selectNote,
                        onSearch = viewModel::onSearchQueryChanged,
                        onClearMessages = viewModel::clearMessages
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesContent(
    state: NotesUiState.Success,
    onAddNote: (String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onStartEditing: (CustomerNote) -> Unit,
    onCancelEditing: () -> Unit,
    onSelectNote: (CustomerNote?) -> Unit,
    onSearch: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    var noteToDelete by remember { mutableStateOf<CustomerNote?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header matching screenshot
        item {
            NotesHeader(notesCount = state.notes.size)
        }

        // Inline Feedback
        if (state.successMessage != null || state.errorMessage != null) {
            item {
                val isSuccess = state.successMessage != null
                val msg = state.successMessage ?: state.errorMessage ?: ""
                val bg = if (isSuccess) SuccessColor.copy(alpha = 0.1f) else ErrorColor.copy(alpha = 0.1f)
                val stroke = if (isSuccess) SuccessColor.copy(alpha = 0.3f) else ErrorColor.copy(alpha = 0.3f)
                val textColor = if (isSuccess) SuccessColor else ErrorColor

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, stroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LaunchedEffect(msg) {
                    delay(4000)
                    onClearMessages()
                }
            }
        }

        // Card 1: New Note / Write a note Form Card
        item {
            NoteInputCard(
                editingNote = state.editingNote,
                isSubmitting = state.isSubmitting,
                onSave = { text ->
                    if (state.editingNote != null) {
                        onUpdateNote(state.editingNote.id, text)
                    } else {
                        onAddNote(text)
                    }
                },
                onCancel = onCancelEditing
            )
        }

        // Card 2 Container for YOUR NOTES & All notes
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header row: "YOUR NOTES", "All notes", and Search notes
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "YOUR NOTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGold,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search notes",
                                    color = BrandGreen.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = BrandGreen.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen.copy(alpha = 0.3f),
                                unfocusedBorderColor = BrandGreen.copy(alpha = 0.1f),
                                focusedContainerColor = CreamSurface,
                                unfocusedContainerColor = CreamSurface,
                                cursorColor = BrandGreen
                            ),
                            singleLine = true
                        )
                    }

                    if (state.filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ምንም ማስታወሻ አልተገኘም።",
                                color = BrandGreen.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.filteredNotes.forEach { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { onSelectNote(note) },
                                    onEdit = { onStartEditing(note) },
                                    onDelete = { noteToDelete = note }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Delete Confirmation Dialog
    noteToDelete?.let { note ->
        val heading = note.note.lineSequence().firstOrNull()?.trim() ?: "ማስታወሻ"
        ConfirmDialog(
            title = "ማስታወሻውን ሰርዝ",
            message = "\"$heading\" መሰረዝ እንደሚፈልጉ እርግጠኛ ነዎት? ይህ ድርጊት አይቀለበስም።",
            onConfirm = {
                onDeleteNote(note.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }

    // Selected Note Reader Notepad Dialog
    state.selectedNote?.let { selected ->
        NoteReaderDialog(
            note = selected,
            onDismiss = { onSelectNote(null) },
            onEdit = {
                onSelectNote(null)
                onStartEditing(selected)
            }
        )
    }
}

@Composable
private fun NotesHeader(notesCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "የግል ማስታወሻ",
                style = MaterialTheme.typography.labelSmall,
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ማስታወሻዎች",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ሀሳብህንና አስፈላጊ መረጃዎችን እዚህ ይመዝግቡ።",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandGreen.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = Color(0xFFE2F0F9),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "$notesCount notes",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2980B9)
            )
        }
    }
}

@Composable
private fun NoteInputCard(
    editingNote: CustomerNote?,
    isSubmitting: Boolean,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(editingNote) {
        mutableStateOf(editingNote?.note ?: "")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.08f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEW NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (editingNote != null) "Edit note" else "Write a note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }

                if (editingNote != null) {
                    TextButton(onClick = onCancel) {
                        Text("ሰርዝ", color = BrandGreen)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGreen.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = {
                        Text(
                            "Write down an idea or important information...",
                            color = BrandGreen.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BrandGreen.copy(alpha = 0.2f),
                        focusedContainerColor = CreamSurface,
                        unfocusedContainerColor = CreamSurface,
                        cursorColor = BrandGreen
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )
            }

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text)
                        if (editingNote == null) text = ""
                    }
                },
                enabled = text.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C5E6B),
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (editingNote != null) "Update note" else "Save note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    note: CustomerNote,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val heading = note.note.lineSequence().firstOrNull()?.trim() ?: "ያልተሰየመ ማስታወሻ"
    val preview = note.note.lineSequence().drop(1).joinToString(" ").trim().ifEmpty { heading }
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CreamSurface,
        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.06f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (preview != heading) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandGreen.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Three-dots menu hidden on the rightmost
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = BrandGreen.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("አስተካክል (Edit)") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandGreen) }
                        )
                        DropdownMenuItem(
                            text = { Text("ሰርዝ (Delete)", color = ErrorColor) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = ErrorColor) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${note.authorName} • ${note.date} ${note.time}".trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandGreen.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun NoteReaderDialog(
    note: CustomerNote,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val heading = note.note.lineSequence().firstOrNull()?.trim() ?: "ማስታወሻ"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(20.dp),
            color = CreamSurface,
            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.1f)),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Notepad Header
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTEPAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandGreen)
                        }
                    }

                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "በ ${note.authorName} • ${note.date} ${note.time}".trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandGreen.copy(alpha = 0.5f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BrandGreen.copy(alpha = 0.1f))
                }

                // Notepad Body (Paper-like view for pure reading)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, BrandGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = note.note,
                                style = MaterialTheme.typography.bodyLarge,
                                color = BrandGreen,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("አስተካክል (Edit)", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ዝጋ (Close)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
