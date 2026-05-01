package com.example.flashlearn.ui.learn

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.repository.DeckRepository
import com.flashlearn.data.dao.DeckWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnHomeViewModel @Inject constructor(
    application: Application,
    private val deckRepository: DeckRepository,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("flashlearn_learn", Context.MODE_PRIVATE)

    private val _decks = MutableStateFlow<List<DeckWithCount>>(emptyList())
    val decks: StateFlow<List<DeckWithCount>> = _decks.asStateFlow()

    private val _lastDeck = MutableStateFlow<DeckWithCount?>(null)
    val lastDeck: StateFlow<DeckWithCount?> = _lastDeck.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.observeAllDecksWithCount().collect { list ->
                _decks.value = list
                // Restore last selected deck from prefs
                val lastId = prefs.getLong(KEY_LAST_DECK_ID, -1L)
                _lastDeck.value = if (lastId >= 0) {
                    list.firstOrNull { it.id == lastId } ?: list.firstOrNull()
                } else {
                    list.firstOrNull()
                }
            }
        }
    }

    /** Called when the user taps a deck to start learning. Persists the choice. */
    fun selectDeck(deck: DeckWithCount) {
        prefs.edit().putLong(KEY_LAST_DECK_ID, deck.id).apply()
        _lastDeck.value = deck
    }

    companion object {
        private const val KEY_LAST_DECK_ID = "last_deck_id"
    }
}
