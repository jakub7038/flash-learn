package com.example.flashlearn.ui.learn

import com.flashlearn.data.entity.Flashcard
import com.flashlearn.data.entity.FlashcardProgress

/**
 * Stan UI sesji nauki.
 */
sealed interface LearnUiState {

    /** Ładowanie kart do sesji. */
    object Loading : LearnUiState

    /** Talia pusta lub brak fiszek do powtórki dziś. */
    data class Empty(val deckTitle: String, val hasCards: Boolean) : LearnUiState

    /** Aktywna sesja nauki. */
    data class Session(
        val deckTitle: String,
        /** Aktualna fiszka. */
        val card: Flashcard,
        /** Postęp SM-2 aktualnej fiszki (null = nowa fiszka bez historii). */
        val progress: FlashcardProgress?,
        /** Numer aktualnej karty (1-based). */
        val currentIndex: Int,
        /** Łączna liczba kart w sesji. */
        val totalCards: Int,
        /** Czy odpowiedź jest już odsłonięta. */
        val isFlipped: Boolean,
    ) : LearnUiState

    /** Sesja zakończona — podsumowanie. */
    data class Finished(
        val deckTitle: String,
        val totalCards: Int,
        val knownCount: Int,   // ocena 3 (Łatwe)
        val hardCount: Int,    // ocena 1 (Trudne)
        val unknownCount: Int, // ocena 0 (Nie wiem)
    ) : LearnUiState
}
