package com.example.flashlearn.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flashlearn.R
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDto
import com.example.flashlearn.ui.marketplace.MarketplaceViewModel
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Definicja kategorii
// ---------------------------------------------------------------------------

private data class Category(
    val slug: String?,
    val labelRes: Int,
    val icon: ImageVector
)

private val CATEGORIES = listOf(
    Category(null,             R.string.marketplace_cat_all,         Icons.Default.Apps),
    Category("jezyki",         R.string.marketplace_cat_languages,   Icons.Default.Language),
    Category("programowanie",  R.string.marketplace_cat_programming, Icons.Default.Code),
    Category("matematyka",     R.string.marketplace_cat_math,        Icons.Default.Calculate),
    Category("nauki-scisle",   R.string.marketplace_cat_science,     Icons.Default.Science),
    Category("historia",       R.string.marketplace_cat_history,     Icons.Default.HistoryEdu),
    Category("inne",           R.string.marketplace_cat_other,       Icons.Default.MoreHoriz),
)

/**
 * Mapuje pole categoryIconName z backendu na ImageVector.
 * Backend zwraca nazwy ikon zgodne z Material Icons (lowercase).
 */
private fun iconForName(name: String?): ImageVector = when (name) {
    "language"     -> Icons.Default.Language
    "code"         -> Icons.Default.Code
    "calculate"    -> Icons.Default.Calculate
    "science"      -> Icons.Default.Science
    "history_edu"  -> Icons.Default.HistoryEdu
    "more_horiz"   -> Icons.Default.MoreHoriz
    "apps"         -> Icons.Default.Apps
    else           -> Icons.Default.Style
}

// ---------------------------------------------------------------------------
// Root screen
// ---------------------------------------------------------------------------

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = hiltViewModel(),
    onDeckClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cloneSuccessMsg  = stringResource(R.string.marketplace_clone_success)
    val cloneErrorPrefix = stringResource(R.string.marketplace_clone_error)

    // Snackbar – sukces klonowania
    LaunchedEffect(uiState.cloneSuccessId) {
        if (uiState.cloneSuccessId != null) {
            scope.launch { snackbarHostState.showSnackbar(cloneSuccessMsg) }
            viewModel.clearCloneSuccess()
        }
    }

    // Snackbar – błąd
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            scope.launch {
                snackbarHostState.showSnackbar("$cloneErrorPrefix: ${uiState.error}")
            }
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ---- Nagłówek ----
            MarketplaceHeader()

            // ---- Chipsy kategorii ----
            CategoryChips(
                selected = uiState.selectedCategory,
                onSelect = viewModel::selectCategory
            )

            // ---- Lista / skeleton ----
            if (uiState.isLoading) {
                SkeletonDeckList()
            } else {
                DeckList(
                    decks       = uiState.decks,
                    cloningId   = uiState.cloningId,
                    onClone     = viewModel::cloneDeck,
                    onRefresh   = viewModel::refresh,
                    onDeckClick = onDeckClick // Przekazanie w dół
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nagłówek
// ---------------------------------------------------------------------------

@Composable
private fun MarketplaceHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.marketplace_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.marketplace_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Chipsy kategorii
// ---------------------------------------------------------------------------

@Composable
private fun CategoryChips(
    selected: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CATEGORIES, key = { it.slug ?: "all" }) { cat ->
            val isSelected = selected == cat.slug
            FilterChip(
                selected  = isSelected,
                onClick   = { onSelect(cat.slug) },
                label     = { Text(stringResource(cat.labelRes)) },
                leadingIcon = {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(50)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Lista talii
// ---------------------------------------------------------------------------

@Composable
private fun DeckList(
    decks: List<MarketplaceDeckDto>,
    cloningId: Long?,
    onClone: (Long) -> Unit,
    onRefresh: () -> Unit,
    onDeckClick: (Long) -> Unit // Dodano parametr
) {
    if (decks.isEmpty()) {
        EmptyMarketplace(onRefresh = onRefresh)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(decks, key = { it.id }) { deck ->
            MarketplaceDeckCard(
                deck      = deck,
                isCloning = cloningId == deck.id,
                onClone   = { onClone(deck.id) },
                onClick   = { onDeckClick(deck.id) } // Przekazanie akcji do karty
            )
        }
        // Mały padding na dole
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Karta talii
// ---------------------------------------------------------------------------

@Composable
private fun MarketplaceDeckCard(
    deck: MarketplaceDeckDto,
    isCloning: Boolean,
    onClone: () -> Unit,
    onClick: () -> Unit // Dodano parametr do sygnatury
) {
    val category = CATEGORIES.firstOrNull { it.slug == deck.categorySlug }
    // Ikona — preferuj categoryIconName zwrócone przez backend, fallback do Icons.Default.Style
    val categoryIcon = iconForName(deck.categoryIconName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Użycie przekazanej lambdy
        shape = RoundedCornerShape(16.dp), // Poprawiono brakujący przecinek
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona kategorii
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Treść
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                // autor (email właściciela)
                Text(
                    text = deck.ownerEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // chipsy z metadanymi
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // kategoria (ikona + nazwa)
                    val catLabel = deck.categoryName ?: deck.categorySlug
                    if (catLabel != null) {
                        DeckMetaChip(icon = categoryIcon, text = catLabel)
                    }
                    // liczba fiszek
                    DeckMetaChip(
                        icon = Icons.Default.Style,
                        text = stringResource(R.string.marketplace_flashcard_count, deck.flashcardCount)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // download_count (pod spodem)
                DeckMetaChip(
                    icon = Icons.Default.Download,
                    text = deck.downloadCount.toString()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Przycisk Klonuj
            FilledTonalButton(
                onClick  = onClone,
                enabled  = !isCloning,
                modifier = Modifier.defaultMinSize(minWidth = 88.dp),
                shape    = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (isCloning) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.marketplace_btn_clone),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckMetaChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Skeleton loading
// ---------------------------------------------------------------------------

@Composable
private fun SkeletonDeckList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(6) {
            SkeletonDeckCard()
        }
    }
}

@Composable
private fun SkeletonDeckCard() {
    val shimmer = shimmerBrush()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ikona placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // tytuł
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // autor
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // chipsy
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmer)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // download chip placeholder (pod spodem)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // przycisk placeholder
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmer)
            )
        }
    }
}

/** Animowany shimmer brush do skeleton UI. */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base    = MaterialTheme.colorScheme.surfaceVariant
    val shimmer = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    return Brush.linearGradient(
        colors = listOf(base, shimmer, base),
        start  = Offset(translateAnim - 200f, 0f),
        end    = Offset(translateAnim + 200f, 0f)
    )
}

// ---------------------------------------------------------------------------
// Pusty stan
// ---------------------------------------------------------------------------

@Composable
private fun EmptyMarketplace(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.marketplace_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.marketplace_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.marketplace_btn_refresh))
        }
    }
}