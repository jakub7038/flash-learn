package com.example.flashlearn.ui.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.dao.FlashcardProgressDao
import com.flashlearn.data.entity.Flashcard
import com.flashlearn.data.entity.FlashcardProgress
import com.example.flashlearn.domain.sm2.SM2Engine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val flashcardProgressDao: FlashcardProgressDao,
    private val deckDao: DeckDao,
    private val syncManager: com.example.flashlearn.sync.SyncManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    // Session data

    /** Ordered queue of flashcards for this session. */
    private var queue: List<Flashcard> = emptyList()
    private var currentIndex = 0

    /** Result counters (grade → count). */
    private var knownCount = 0
    private var hardCount = 0
    private var unknownCount = 0

    private var deckTitle = ""

    // Init

    init {
        viewModelScope.launch {
            loadSession()
        }
    }

    // Public API

    /** Called when the user taps the card to reveal the answer. */
    fun flipCard() {
        val current = _uiState.value as? LearnUiState.Session ?: return
        _uiState.value = current.copy(isFlipped = true)
    }

    /**
     * Called when user selects a rating.
     * @param grade 0 = Nie wiem, 1 = Trudne, 3 = Łatwe
     */
    fun rateCard(grade: Int) {
        val current = _uiState.value as? LearnUiState.Session ?: return

        viewModelScope.launch {
            saveProgress(card = current.card, currentProgress = current.progress, grade = grade)
            when (grade) {
                0 -> unknownCount++
                1 -> hardCount++
                else -> knownCount++
            }
            advance()
        }
    }

    /**
     * Restarts the session with all cards from the deck (ignores SM-2 due filter).
     * Called when user taps "Repeat session" on the Finished screen.
     */
    fun restartSession() {
        viewModelScope.launch {
            _uiState.value = LearnUiState.Loading
            knownCount = 0
            hardCount = 0
            unknownCount = 0
            val allCards = flashcardDao.getByDeck(deckId)
            if (allCards.isEmpty()) {
                _uiState.value = LearnUiState.Empty(deckTitle, hasCards = false)
                return@launch
            }
            queue = allCards
            currentIndex = 0
            showCard()
        }
    }

    // Private helpers

    private suspend fun loadSession() {
        _uiState.value = LearnUiState.Loading

        // Fetch deck title
        val deck = deckDao.getById(deckId)
        deckTitle = deck?.title ?: ""

        // Load all flashcards for the deck
        val allCards = flashcardDao.getByDeck(deckId)

        // Filter to due cards (nextReviewEpochDay <= today) + brand new cards (no progress record)
        val today = LocalDate.now().toEpochDay()
        val dueCards = allCards.filter { card ->
            val progress = flashcardProgressDao.getByFlashcard(card.id)
            progress == null || progress.nextReviewEpochDay <= today
        }

        if (dueCards.isEmpty()) {
            _uiState.value = LearnUiState.Empty(deckTitle, hasCards = allCards.isNotEmpty())
            return
        }

        queue = dueCards
        currentIndex = 0
        showCard()
    }

    private suspend fun showCard() {
        if (currentIndex >= queue.size) {
            _uiState.value = LearnUiState.Finished(
                deckTitle = deckTitle,
                totalCards = queue.size,
                knownCount = knownCount,
                hardCount = hardCount,
                unknownCount = unknownCount,
            )
            syncManager.scheduleSync()
            return
        }

        val card = queue[currentIndex]
        val progress = flashcardProgressDao.getByFlashcard(card.id)

        _uiState.value = LearnUiState.Session(
            deckTitle = deckTitle,
            card = card,
            progress = progress,
            currentIndex = currentIndex + 1,
            totalCards = queue.size,
            isFlipped = false,
        )
    }

    private suspend fun advance() {
        currentIndex++
        showCard()
    }

    private suspend fun saveProgress(
        card: Flashcard,
        currentProgress: FlashcardProgress?,
        grade: Int,
    ) {
        val ef = currentProgress?.easeFactor ?: SM2Engine.DEFAULT_EASE_FACTOR
        val interval = currentProgress?.intervalDays ?: 1
        val reps = currentProgress?.repetitions ?: 0

        val result = SM2Engine.calculate(
            grade = grade,
            easeFactor = ef,
            intervalDays = interval,
            repetitions = reps,
        )

        if (currentProgress == null) {
            // First time — insert a new record
            flashcardProgressDao.insert(
                FlashcardProgress(
                    flashcardId = card.id,
                    easeFactor = result.easeFactor,
                    intervalDays = result.intervalDays,
                    repetitions = result.repetitions,
                    nextReviewEpochDay = result.nextReviewDate.toEpochDay(),
                )
            )
        } else {
            flashcardProgressDao.update(
                currentProgress.copy(
                    easeFactor = result.easeFactor,
                    intervalDays = result.intervalDays,
                    repetitions = result.repetitions,
                    nextReviewEpochDay = result.nextReviewDate.toEpochDay(),
                )
            )
        }
    }
}
