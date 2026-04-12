package com.example.flashlearn.ui.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.data.dao.DeckWithCount
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.dao.FlashcardProgressDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val flashcardProgressDao: FlashcardProgressDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _deck = MutableStateFlow<DeckWithCount?>(null)
    val deck: StateFlow<DeckWithCount?> = _deck

    val totalFlashcards: StateFlow<Int> = flashcardDao.observeCountByDeck(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueTodayCount: StateFlow<Int> = flashcardProgressDao.observeDueCount(deckId, LocalDate.now().toEpochDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteryPercentage: StateFlow<Float> = combine(
        totalFlashcards,
        flashcardProgressDao.observeProgressByDeck(deckId)
    ) { total, progressList ->
        if (total == 0) return@combine 0f
        
        // Fiszka uznana za "opanowaną" jeśli interwał >= 21 dni
        val masteredCount = progressList.count { it.intervalDays >= 21 }
        
        (masteredCount.toFloat() / total.toFloat()) * 100f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            // DeckWithCount jako UI state dla pojedynczego widoku
            // Pobieramy całą listę i filtrujemy lokalnie
            deckDao.observeAllWithCount().map { list -> 
                list.find { it.id == deckId }
            }.collect {
                _deck.value = it
            }
        }
    }
}
