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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flashlearn.R
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Deck
import kotlinx.coroutines.launch
import java.time.Instant

private const val TITLE_MIN = 3
private const val TITLE_MAX = 100
private const val DESC_MAX = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditScreen(
    deckId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).deckDao() }
    val scope = rememberCoroutineScope()

    val isEditMode = deckId != null

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }

    val errTitleRequired = stringResource(R.string.error_title_required)
    val errTitleMinLength = stringResource(R.string.error_title_min_length)
    val errTitleMaxLength = stringResource(R.string.error_title_max_length)

    LaunchedEffect(deckId) {
        if (deckId != null) {
            val deck = dao.getById(deckId)
            if (deck != null) {
                title = deck.title
                description = deck.description ?: ""
            }
            isLoading = false
        }
    }

    fun validateTitle(): Boolean {
        titleError = when {
            title.isBlank() -> errTitleRequired
            title.trim().length < TITLE_MIN -> String.format(errTitleMinLength, TITLE_MIN)
            title.length > TITLE_MAX -> String.format(errTitleMaxLength, TITLE_MAX)
            else -> null
        }
        return titleError == null
    }

    fun save() {
        if (!validateTitle()) return

        isSaving = true
        scope.launch {
            val now = Instant.now().epochSecond
            if (isEditMode && deckId != null) {
                val existing = dao.getById(deckId)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            updatedAt = now,
                            needsSync = true
                        )
                    )
                }
            } else {
                dao.insert(
                    Deck(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        needsSync = true
                    )
                )
            }
            com.example.flashlearn.sync.SyncManager(context.applicationContext).scheduleSync()
            isSaving = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditMode) R.string.deck_edit_title else R.string.deck_new_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
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
                            contentDescription = stringResource(R.string.content_desc_save)
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
                    value = title,
                    onValueChange = {
                        if (it.length <= TITLE_MAX) title = it
                        if (titleError != null) validateTitle()
                    },
                    label = { Text(stringResource(R.string.label_deck_name)) },
                    isError = titleError != null,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = titleError ?: "",
                                color = if (titleError != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${title.length}/$TITLE_MAX",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        if (it.length <= DESC_MAX) description = it
                    },
                    label = { Text(stringResource(R.string.label_description_optional)) },
                    supportingText = {
                        Text(
                            text = "${description.length}/$DESC_MAX",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
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
                        Text(stringResource(if (isEditMode) R.string.btn_save_changes else R.string.btn_create_deck))
                    }
                }
            }
        }
    }
}
