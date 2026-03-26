package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.launch
import java.time.Instant

private const val FIELD_MAX = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardEditScreen(
    deckId: Long? = null,
    flashcardId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).flashcardDao() }
    val scope = rememberCoroutineScope()

    val isEditMode = flashcardId != null

    var resolvedDeckId by remember { mutableStateOf(deckId ?: 0L) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var questionError by remember { mutableStateOf<String?>(null) }
    var answerError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(flashcardId) {
        if (flashcardId != null) {
            val flashcard = dao.getById(flashcardId)
            if (flashcard != null) {
                resolvedDeckId = flashcard.deckId
                question = flashcard.question
                answer = flashcard.answer
            }
            isLoading = false
        }
    }

    fun validateQuestion(): Boolean {
        questionError = when {
            question.isBlank() -> "Pytanie jest wymagane"
            question.length > FIELD_MAX -> "Pytanie może mieć maksymalnie $FIELD_MAX znaków"
            else -> null
        }
        return questionError == null
    }

    fun validateAnswer(): Boolean {
        answerError = when {
            answer.isBlank() -> "Odpowiedź jest wymagana"
            answer.length > FIELD_MAX -> "Odpowiedź może mieć maksymalnie $FIELD_MAX znaków"
            else -> null
        }
        return answerError == null
    }

    fun save() {
        val questionOk = validateQuestion()
        val answerOk = validateAnswer()
        if (!questionOk || !answerOk) return

        isSaving = true
        scope.launch {
            val now = Instant.now().epochSecond
            if (isEditMode && flashcardId != null) {
                val existing = dao.getById(flashcardId)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            question = question.trim(),
                            answer = answer.trim(),
                            updatedAt = now,
                            needsSync = true
                        )
                    )
                }
            } else {
                dao.insert(
                    Flashcard(
                        deckId = resolvedDeckId,
                        question = question.trim(),
                        answer = answer.trim(),
                        needsSync = true
                    )
                )
            }
            isSaving = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Edytuj fiszkę" else "Nowa fiszka")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { save() },
                        enabled = !isLoading && !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Zapisz"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        if (it.length <= FIELD_MAX) question = it
                        if (questionError != null) validateQuestion()
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
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        if (it.length <= FIELD_MAX) answer = it
                        if (answerError != null) validateAnswer()
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
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { save() },
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
                        Text(if (isEditMode) "Zapisz zmiany" else "Utwórz fiszkę")
                    }
                }
            }
        }
    }
}
