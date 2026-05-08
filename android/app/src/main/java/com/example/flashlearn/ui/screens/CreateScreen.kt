package com.example.flashlearn.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flashlearn.R
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

    var selectedDeckId by remember { mutableStateOf<Long?>(null) }
    val selectedDeck = remember(selectedDeckId, decks) {
        decks.find { it.id == selectedDeckId } ?: decks.firstOrNull()
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var questionError by remember { mutableStateOf<String?>(null) }
    var answerError by remember { mutableStateOf<String?>(null) }
    var deckError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val errDeckRequired = stringResource(R.string.error_deck_required)
    val errQuestionRequired = stringResource(R.string.error_question_required)
    val errAnswerRequired = stringResource(R.string.error_answer_required)
    val errFieldMaxChars = stringResource(R.string.error_field_max_chars)
    val msgFlashcardAdded = stringResource(R.string.flashcard_added)

    LaunchedEffect(decks) {
        if (selectedDeckId == null && decks.isNotEmpty()) {
            selectedDeckId = decks.first().id
        }
    }

    fun validateAndSave() {
        deckError = if (selectedDeck == null) errDeckRequired else null
        questionError = when {
            question.isBlank() -> errQuestionRequired
            question.length > FIELD_MAX -> String.format(errFieldMaxChars, FIELD_MAX)
            else -> null
        }
        answerError = when {
            answer.isBlank() -> errAnswerRequired
            answer.length > FIELD_MAX -> String.format(errFieldMaxChars, FIELD_MAX)
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
            snackbarHostState.showSnackbar(msgFlashcardAdded)
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
                    text = stringResource(R.string.create_flashcard_title),
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
                        label = { Text(stringResource(R.string.label_deck)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        isError = deckError != null,
                        supportingText = deckError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        decks.forEach { deck ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = deck.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.flashcards_count, deck.flashcardCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedDeckId = deck.id // Zapisujemy nowe ID!
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
                    label = { Text(stringResource(R.string.label_question)) },
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
                    label = { Text(stringResource(R.string.label_answer)) },
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
                        Text(stringResource(R.string.btn_add_flashcard))
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
            text = stringResource(R.string.no_decks_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.no_decks_create_first),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
