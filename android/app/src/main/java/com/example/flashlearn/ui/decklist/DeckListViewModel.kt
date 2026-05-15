package com.example.flashlearn.ui.decklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.CategoryDto
import com.example.flashlearn.data.repository.CategoryRepository
import com.example.flashlearn.data.repository.DeckRepository
import com.flashlearn.data.dao.DeckWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckListViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories

    val decks: StateFlow<List<DeckWithCount>> = deckRepository
        .observeAllDecksWithCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _categories.value = runCatching { categoryRepository.getCategories() }
                .getOrDefault(emptyList())
        }
    }

    fun createDeck(title: String, description: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            deckRepository.createDeck(title.trim(), description?.trim()?.ifBlank { null })
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            deckRepository.deleteDeckById(deckId)
        }
    }
}
