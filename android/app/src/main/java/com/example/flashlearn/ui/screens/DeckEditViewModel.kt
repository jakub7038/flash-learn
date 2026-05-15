package com.example.flashlearn.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.CategoryDto
import com.example.flashlearn.data.repository.CategoryRepository
import com.example.flashlearn.data.repository.DeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TITLE_MIN_LEN = 3
private const val TITLE_MAX_LEN = 100
private const val DESC_MAX_LEN  = 500

data class DeckEditUiState(
    val title: String = "",
    val description: String = "",
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategorySlug: String? = null,
    val titleError: String? = null,
    val isLoading: Boolean = false,
    val isCategoriesLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class DeckEditViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** null means "create new deck" */
    private val deckId: Long? = savedStateHandle.get<Long>("deckId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(DeckEditUiState(isLoading = deckId != null))
    val uiState: StateFlow<DeckEditUiState> = _uiState

    init {
        loadCategories()
        loadDeckIfEditing()
    }

    private fun loadCategories() {
        _uiState.value = _uiState.value.copy(isCategoriesLoading = true)
        viewModelScope.launch {
            val categories = runCatching { categoryRepository.getCategories() }
                .getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                categories = categories,
                isCategoriesLoading = false
            )
        }
    }

    private fun loadDeckIfEditing() {
        val id = deckId ?: return
        viewModelScope.launch {
            val deck = deckRepository.getDeckById(id)
            _uiState.value = if (deck != null) {
                _uiState.value.copy(
                    title = deck.title,
                    description = deck.description ?: "",
                    selectedCategorySlug = deck.categorySlug,
                    isLoading = false
                )
            } else {
                _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value.take(TITLE_MAX_LEN),
            titleError = null
        )
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value.take(DESC_MAX_LEN))
    }

    fun onCategorySelected(slug: String?) {
        _uiState.value = _uiState.value.copy(selectedCategorySlug = slug)
    }

    fun save(
        errTitleRequired: String,
        errTitleMinLength: String,
        errTitleMaxLength: String
    ) {
        val state = _uiState.value
        val titleError = when {
            state.title.isBlank()                       -> errTitleRequired
            state.title.trim().length < TITLE_MIN_LEN   -> String.format(errTitleMinLength, TITLE_MIN_LEN)
            state.title.length > TITLE_MAX_LEN          -> String.format(errTitleMaxLength, TITLE_MAX_LEN)
            else                                        -> null
        }
        if (titleError != null) {
            _uiState.value = state.copy(titleError = titleError)
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val trimmedTitle = state.title.trim()
            val trimmedDesc  = state.description.trim().ifBlank { null }

            if (deckId != null) {
                deckRepository.updateDeck(deckId, trimmedTitle, trimmedDesc, state.selectedCategorySlug)
            } else {
                deckRepository.createDeck(trimmedTitle, trimmedDesc, state.selectedCategorySlug)
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    companion object {
        const val TITLE_MAX = TITLE_MAX_LEN
        const val DESC_MAX  = DESC_MAX_LEN
    }
}
