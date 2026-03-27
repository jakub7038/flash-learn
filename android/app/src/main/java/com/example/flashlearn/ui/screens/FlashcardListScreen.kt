package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    deckId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCreateFlashcard: () -> Unit,
    onNavigateToEditFlashcard: (flashcardId: Long) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val flashcardDao = remember { db.flashcardDao() }
    val deckDao = remember { db.deckDao() }
    val scope = rememberCoroutineScope()

    val flashcards by flashcardDao.observeByDeck(deckId).collectAsState(initial = emptyList())
    var deckTitle by remember { mutableStateOf("") }
    var flashcardToDelete by remember { mutableStateOf<Flashcard?>(null) }

    LaunchedEffect(deckId) {
        deckTitle = deckDao.getById(deckId)?.title ?: ""
    }

    if (flashcardToDelete != null) {
        AlertDialog(
            onDismissRequest = { flashcardToDelete = null },
            title = { Text("Usuń fiszkę") },
            text = { Text("Czy na pewno chcesz usunąć tę fiszkę? Tej operacji nie można cofnąć.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = flashcardToDelete!!
                        flashcardToDelete = null
                        scope.launch { flashcardDao.delete(target) }
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { flashcardToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateFlashcard,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj fiszkę")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyFlashcardsState(onAddClick = onNavigateToCreateFlashcard)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Fiszki: ${flashcards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(flashcards, key = { it.id }) { flashcard ->
                    FlashcardItem(
                        flashcard = flashcard,
                        onClick = { onNavigateToEditFlashcard(flashcard.id) },
                        onDeleteClick = { flashcardToDelete = flashcard }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardItem(
    flashcard: Flashcard,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flashcard.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = flashcard.answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń fiszkę",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyFlashcardsState(onAddClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "Brak fiszek",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Dodaj pierwszą fiszkę do tej talii",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dodaj fiszkę")
        }
    }
}
