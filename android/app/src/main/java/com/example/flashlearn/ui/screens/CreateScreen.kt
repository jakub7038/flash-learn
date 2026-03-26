package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.ui.decklist.DeckListViewModel
import com.flashlearn.data.dao.DeckWithCount
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.launch
import java.time.Instant

private const val FIELD_MAX = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: DeckListViewModel = hiltViewModel()
) {
    val decks by viewModel.decks.collectAsState()
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).flashcardDao() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDeck by remember { mutableStateOf<DeckWithCount?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var questionError by remember { mutableStateOf<String?>(null) }
    var answerError by remember { mutableStateOf<String?>(null) }
    var deckError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(decks) {
        if (selectedDeck == null && decks.isNotEmpty()) {
            selectedDeck = decks.first()
        }
    }

    fun validateAndSave() {
        deckError = if (selectedDeck == null) "Wybierz talię" else null
        questionError = when {
            question.isBlank() -> "Pytanie jest wymagane"
            question.length > FIELD_MAX -> "Maksymalnie $FIELD_MAX znaków"
            else -> null
        }
        answerError = when {
            answer.isBlank() -> "Odpowiedź jest wymagana"
            answer.length > FIELD_MAX -> "Maksymalnie $FIELD_MAX znaków"
            else -> null
        }
        if (deckError != null || questionError != null || answerError != null) return

        isSaving = true
        scope.launch {
            dao.insert(
                Flashcard(
                    deckId = selectedDeck!!.id,
                    question = question.trim(),
                    answer = answer.trim(),
                    createdAt = Instant.now().epochSecond,
                    updatedAt = Instant.now().epochSecond,
                    needsSync = true
                )
            )
            question = ""
            answer = ""
            questionError = null
            answerError = null
            isSaving = false
            snackbarHostState.showSnackbar("Fiszka dodana!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (decks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                NoDecksPrompt()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Dodaj fiszkę",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Deck selector
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDeck?.title ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Talia") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        isError = deckError != null,
                        supportingText = deckError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        decks.forEach { deck ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = deck.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Fiszki: ${deck.flashcardCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedDeck = deck
                                    deckError = null
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Question field
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        if (it.length <= FIELD_MAX) question = it
                        if (questionError != null && it.isNotBlank()) questionError = null
                    },
                    label = { Text("Pytanie") },
                    isError = questionError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = questionError ?: "",
                                color = if (questionError != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${question.length}/$FIELD_MAX",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                // Answer field
                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        if (it.length <= FIELD_MAX) answer = it
                        if (answerError != null && it.isNotBlank()) answerError = null
                    },
                    label = { Text("Odpowiedź") },
                    isError = answerError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = answerError ?: "",
                                color = if (answerError != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${answer.length}/$FIELD_MAX",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { validateAndSave() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Dodaj fiszkę")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoDecksPrompt() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Text(
            text = "Brak talii",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Najpierw utwórz talię w zakładce \"Moje talie\", aby móc dodawać fiszki.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
