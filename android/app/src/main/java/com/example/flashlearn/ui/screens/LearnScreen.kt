package com.example.flashlearn.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flashlearn.R
import com.example.flashlearn.ui.learn.LearnUiState
import com.example.flashlearn.ui.learn.LearnViewModel

@Composable
fun LearnScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            LearnUiState.Loading -> LearnLoadingContent()
            is LearnUiState.Empty -> LearnEmptyContent(
                state = state,
                onNavigateBack = onNavigateBack,
                onLearnAnyway = viewModel::restartSession
            )
            is LearnUiState.Session -> LearnSessionContent(
                state = state,
                onFlip = viewModel::flipCard,
                onRate = viewModel::rateCard,
                onNavigateBack = onNavigateBack,
            )
            is LearnUiState.Finished -> LearnFinishedContent(
                state = state,
                onNavigateBack = onNavigateBack,
                onRestart = viewModel::restartSession,
            )
        }
    }
}

// Loading

@Composable
private fun LearnLoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// Empty

@Composable
private fun LearnEmptyContent(
    state: LearnUiState.Empty,
    onNavigateBack: () -> Unit,
    onLearnAnyway: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (state.hasCards) stringResource(R.string.learn_great_job) else stringResource(R.string.empty_flashcards_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.hasCards) stringResource(R.string.learn_empty_message, state.deckTitle)
            else stringResource(R.string.empty_flashcards_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        if (state.hasCards) {
            Button(
                onClick = onLearnAnyway,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.learn_repeat_session), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.learn_back_to_menu), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.learn_back_to_menu), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnSessionContent(
    state: LearnUiState.Session,
    onFlip: () -> Unit,
    onRate: (Int) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.deckTitle.ifEmpty { stringResource(R.string.learn_session_title) },
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(8.dp))

            // Progress bar
            SessionProgressBar(
                current = state.currentIndex,
                total = state.totalCards,
            )

            Spacer(Modifier.height(32.dp))

            // Flip card
            FlipCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                question = state.card.question,
                answer = state.card.answer,
                isFlipped = state.isFlipped,
                onClick = { if (!state.isFlipped) onFlip() },
            )

            Spacer(Modifier.height(28.dp))

            // Rating buttons (visible only after flip)
            RatingButtons(
                visible = state.isFlipped,
                onRate = onRate,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Progress bar

@Composable
private fun SessionProgressBar(current: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = current.toFloat() / total.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "sessionProgress",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.learn_progress, current, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${((current.toFloat() / total) * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// Flip card

@Composable
private fun FlipCard(
    modifier: Modifier = Modifier,
    question: String,
    answer: String,
    isFlipped: Boolean,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "cardFlip",
    )

    Box(
        modifier = modifier
            .graphicsLayer { cameraDistance = 14f * density }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Front face (question)
        CardFace(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    alpha = if (rotation <= 90f) 1f else 0f
                },
            isFront = true,
            content = question,
            hint = stringResource(R.string.learn_hint_front),
        )

        // Back face (answer)
        CardFace(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation - 180f
                    alpha = if (rotation > 90f) 1f else 0f
                },
            isFront = false,
            content = answer,
            hint = stringResource(R.string.learn_hint_back),
        )
    }
}

@Composable
private fun CardFace(
    modifier: Modifier = Modifier,
    isFront: Boolean,
    content: String,
    hint: String,
) {
    val gradient = if (isFront) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Label chip
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            ) {
                Text(
                    text = if (isFront) stringResource(R.string.learn_label_question) else stringResource(R.string.learn_label_answer),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isFront) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (isFront) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = (if (isFront) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onTertiaryContainer).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Rating buttons

@Composable
private fun RatingButtons(visible: Boolean, onRate: (Int) -> Unit) {
    val targetAlpha = if (visible) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(300),
        label = "ratingAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Łatwe — grade 3
        RatingButton(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.learn_rate_easy),
            emoji = "😎",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = { if (visible) onRate(3) },
        )
        // Trudne — grade 1
        RatingButton(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.learn_rate_hard),
            emoji = "🤔",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = { if (visible) onRate(1) },
        )
        // Nie wiem — grade 0
        RatingButton(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.learn_rate_unknown),
            emoji = "🤯",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onClick = { if (visible) onRate(0) },
        )
    }
}
@Composable
private fun RatingButton(
    modifier: Modifier = Modifier,
    label: String,
    emoji: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}


@Composable
private fun LearnFinishedContent(
    state: LearnUiState.Finished,
    onNavigateBack: () -> Unit,
    onRestart: () -> Unit,
) {
    val textNone = stringResource(R.string.learn_next_session_none)
    val textToday = stringResource(R.string.learn_next_session_today)
    val textTomorrow = stringResource(R.string.learn_next_session_tomorrow)
    val textDayAfter = stringResource(R.string.learn_next_session_day_after)

    val nextSessionText = remember(state.nextSessionEpochDay) {
        if (state.nextSessionEpochDay == null) textNone
        else {
            val today = java.time.LocalDate.now().toEpochDay()
            val diff = state.nextSessionEpochDay - today
            when {
                diff <= 0 -> textToday
                diff == 1L -> textTomorrow
                diff == 2L -> textDayAfter
                else -> {
                    val date = java.time.LocalDate.ofEpochDay(state.nextSessionEpochDay)
                    date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.learn_session_finished),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.deckTitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.learn_cards_completed, state.totalCards),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryChip(
                modifier = Modifier.weight(1f),
                emoji = "😎",
                label = stringResource(R.string.learn_rate_easy),
                count = state.knownCount,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                emoji = "🤔",
                label = stringResource(R.string.learn_rate_hard),
                count = state.hardCount,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                emoji = "🤯",
                label = stringResource(R.string.learn_rate_unknown),
                count = state.unknownCount,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.learn_next_session_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nextSessionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.learn_repeat_session), style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.learn_back_to_decks), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
            )
        }
    }
}