package com.chuledigita.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.ui.components.EmptyStatePlaceholder
import com.chuledigita.mobile.ui.components.MainScaffold
import com.chuledigita.mobile.ui.theme.SuccessColor
import kotlinx.coroutines.delay

/**
 * Main global Business Notes screen connected to the shared backend API.
 * Displays shared family notes with creator attribution and creation timestamps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    MainScaffold(
        navController = navController,
        title = "ማስታወሻዎች",
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "ማስታወሻ አክል")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is NotesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NotesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadNotes() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("እንደገና ሞክር")
                            }
                        }
                    }
                }
                is NotesUiState.Empty -> {
                    EmptyStatePlaceholder(
                        icon = Icons.Outlined.Description,
                        text = "ምንም ማስታወሻ አልተመዘገበም።",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NotesUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Success Feedback Banner
                        state.successMessage?.let { msg ->
                            Surface(
                                color = SuccessColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = SuccessColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SuccessColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            LaunchedEffect(msg) {
                                delay(5000)
                                viewModel.clearSuccessMessage()
                            }
                        }

                        // Header Description
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ማስታወሻዎች",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "የቤተሰብ አባላት ማስታወሻ መመዝገብና ሌሎች አባላት ማንበብ ይችላሉ።",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.notes, key = { it.id }) { note ->
                                BusinessNoteCard(note = note)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddNoteDialog(
                isSubmitting = (uiState as? NotesUiState.Success)?.isCreating ?: false,
                onDismiss = { showAddDialog = false },
                onNoteAdded = { content ->
                    viewModel.addNote(content)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BusinessNoteCard(note: CustomerNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = note.note,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Creator Info - Visually prominent (Requirement 6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "የመዘገበው: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = note.authorName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date (Requirement 6 & 7)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📅 ${note.date}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    // Time (Requirement 6 & 7)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🕒 ${note.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddNoteDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onNoteAdded: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = if (isSubmitting) ({}) else onDismiss,
        title = {
            Text(
                text = "አዲስ ማስታወሻ መዝግብ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { 
                        content = it
                        if (it.isNotBlank()) showError = false
                    },
                    label = { Text("ማስታወሻ") },
                    placeholder = { Text("ማስታወሻዎን እዚህ ይጻፉ...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    isError = showError,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (showError) {
                            Text("ማስታወሻ ያስገቡ።", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                if (isSubmitting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isBlank()) {
                        showError = true
                    } else {
                        onNoteAdded(content)
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("ማስታወሻ መዝግብ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("ሰርዝ")
            }
        }
    )
}
