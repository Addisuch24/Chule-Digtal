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
import com.chuledigita.mobile.ui.components.MultiSelectActionBar
import com.chuledigita.mobile.ui.components.MultiSelectDeleteConfirmationDialog
import com.chuledigita.mobile.ui.components.NotesHeaderIllustration
import com.chuledigita.mobile.ui.components.ScreenHeaderBanner
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
                        onClearMessages = viewModel::clearMessages,
                        onToggleSelectNote = viewModel::toggleSelectNote,
                        onSelectAllNotes = viewModel::selectAllNotes,
                        onClearSelection = viewModel::clearNoteSelection,
                        onBulkDeleteSelected = viewModel::bulkDeleteSelectedNotes
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
    onClearMessages: () -> Unit,
    onToggleSelectNote: (String) -> Unit,
    onSelectAllNotes: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onBulkDeleteSelected: (((Int, String) -> Unit)?) -> Unit
) {
    var noteToDelete by remember { mutableStateOf<CustomerNote?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

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
                color = PureWhiteSurface,
                border = BorderStroke(1.dp, BorderDivider),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header row: "YOUR NOTES", "All notes", and Search notes
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "YOUR NOTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All notes (${state.filteredNotes.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Search notes...",
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = BorderDivider,
                                focusedContainerColor = OffWhiteBg,
                                unfocusedContainerColor = OffWhiteBg,
                                cursorColor = EmeraldGreen,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy
                            ),
                            singleLine = true
                        )
                    }

                    MultiSelectActionBar(
                        selectedCount = state.selectedNoteIds.size,
                        totalCount = state.filteredNotes.size,
                        isDeleting = state.isDeletingBatch,
                        onSelectAll = { onSelectAllNotes(state.filteredNotes.map { it.id }) },
                        onClearSelection = onClearSelection,
                        onDeleteSelected = { showBulkDeleteDialog = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

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
                                    onDelete = { noteToDelete = note },
                                    isSelected = state.selectedNoteIds.contains(note.id),
                                    isSelectionActive = state.selectedNoteIds.isNotEmpty(),
                                    onToggleSelect = { onToggleSelectNote(note.id) }
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

    if (showBulkDeleteDialog) {
        MultiSelectDeleteConfirmationDialog(
            itemType = "note",
            selectedCount = state.selectedNoteIds.size,
            warningMessage = "This action cannot be undone.",
            isDeleting = state.isDeletingBatch,
            onConfirm = {
                onBulkDeleteSelected { _, _ ->
                    showBulkDeleteDialog = false
                }
            },
            onDismiss = {
                if (!state.isDeletingBatch) showBulkDeleteDialog = false
            }
        )
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
    ScreenHeaderBanner(
        title = "Notes",
        subtitle = "Keep important information and reminders.",
        illustration = {
            NotesHeaderIllustration()
        }
    )
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
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, BorderDivider),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = LightMint,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (editingNote != null) "Edit note" else "Write a new note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                }

                if (editingNote != null) {
                    TextButton(onClick = onCancel) {
                        Text("ሰርዝ", color = ErrorColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                placeholder = {
                    Text(
                        "Enter your note...",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = BorderDivider,
                    focusedContainerColor = OffWhiteBg,
                    unfocusedContainerColor = OffWhiteBg,
                    cursorColor = EmeraldGreen,
                    focusedTextColor = BrandNavy,
                    unfocusedTextColor = BrandNavy
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

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
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (editingNote != null) "Update note" else "Save note",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionActive: Boolean = false,
    onToggleSelect: (() -> Unit)? = null
) {
    val heading = note.note.lineSequence().firstOrNull()?.trim() ?: "ያልተሰየመ ማስታወሻ"
    val preview = note.note.lineSequence().drop(1).joinToString(" ").trim().ifEmpty { heading }
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (isSelectionActive && onToggleSelect != null) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) LightMint.copy(alpha = 0.5f) else PureWhiteSurface,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) EmeraldGreen else BorderDivider
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onToggleSelect != null) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldGreen,
                            uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (preview != heading) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Three-dots menu
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
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
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = EmeraldGreen) }
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
                    color = TextSecondary.copy(alpha = 0.8f)
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
