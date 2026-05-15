package com.example.flashlearn.ui.publish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.CategoryDto
import com.example.flashlearn.data.repository.CategoryRepository
import com.example.flashlearn.data.repository.MarketplaceRepository
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.DeckWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.flashlearn.sync.SyncManager

sealed interface PublishUiState {
    /** Trwa ładowanie kategorii / talii */
    object Loading : PublishUiState

    /** Dane załadowane — formularz gotowy */
    data class Ready(
        val deckTitle: String,
        val deckServerId: Long?,          // null gdy talia nie jest zsynchronizowana
        val isSyncing: Boolean,
        val categories: List<CategoryDto>,
        val selectedCategoryId: Long?,
        val description: String,
        val isSubmitting: Boolean = false
    ) : PublishUiState

    /** Publikacja zakończyła się sukcesem */
    object Success : PublishUiState

    /** Błąd ładowania */
    data class Error(val message: String) : PublishUiState
}

@HiltViewModel
class PublishDeckViewModel @Inject constructor(
    private val deckDao: DeckDao,
    private val categoryRepository: CategoryRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val syncManager: SyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Loading)
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    /** Jednorazowe zdarzenie błędu submitu (do Snackbar) */
    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    init {
        syncManager.scheduleSync()
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                categoryRepository.getCategories()
            }.onSuccess { categories ->
                deckDao.observeById(deckId).collect { deck ->
                    if (deck == null) {
                        _uiState.value = PublishUiState.Error("Talia o id=$deckId nie istnieje")
                        return@collect
                    }
                    val current = _uiState.value
                    if (current is PublishUiState.Ready && current.isSubmitting) return@collect
                    if (current is PublishUiState.Success) return@collect

                    _uiState.value = PublishUiState.Ready(
                        deckTitle = deck.title,
                        deckServerId = deck.serverId,
                        isSyncing = deck.serverId == null,
                        categories = categories,
                        selectedCategoryId = (current as? PublishUiState.Ready)?.selectedCategoryId
                            ?: categories.firstOrNull()?.id,
                        description = (current as? PublishUiState.Ready)?.description
                            ?: deck.description.orEmpty()
                    )
                }
            }.onFailure { e ->
                _uiState.value = PublishUiState.Error(
                    e.message ?: "Błąd ładowania danych"
                )
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        _uiState.value = s.copy(selectedCategoryId = categoryId)
    }

    fun setDescription(text: String) {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        _uiState.value = s.copy(description = text)
    }

    fun submit() {
        val s = _uiState.value as? PublishUiState.Ready ?: return
        val categoryId = s.selectedCategoryId ?: run {
            _submitError.value = "Wybierz kategorię"
            return
        }
        val serverId = s.deckServerId ?: run {
            _submitError.value = "Talia nie jest jeszcze zsynchronizowana z serwerem. Spróbuj za chwilę."
            return
        }

        _uiState.value = s.copy(isSubmitting = true)

        viewModelScope.launch {
            runCatching {
                marketplaceRepository.submitDeck(
                    deckServerId = serverId,
                    categoryId = categoryId,
                    description = s.description
                )
            }.onSuccess {
                _uiState.value = PublishUiState.Success
            }.onFailure { e ->
                // Przywróć stan Ready, przekaż błąd przez Snackbar
                _uiState.value = s.copy(isSubmitting = false)
                _submitError.value = e.message ?: "Publikacja nie powiodła się"
            }
        }
    }

    fun clearSubmitError() {
        _submitError.value = null
    }
}
