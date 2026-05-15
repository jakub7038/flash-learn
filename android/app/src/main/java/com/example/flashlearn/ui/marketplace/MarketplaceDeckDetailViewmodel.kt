package com.example.flashlearn.ui.marketplace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDetailsDto
import com.example.flashlearn.data.repository.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val deck: MarketplaceDeckDetailsDto? = null,
    val isCloning: Boolean = false,
    val isReporting: Boolean = false,
    val error: String? = null
)

sealed class DetailUiEvent {
    data class ShowSnackbar(val message: String) : DetailUiEvent()
    object NavigateBack : DetailUiEvent()
    object NavigateToLocalDecks : DetailUiEvent()
}

@HiltViewModel
class MarketplaceDeckDetailViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Założenie: w Route definujesz argument "deckId"
    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<DetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadDeckDetails()
    }

    private fun loadDeckDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getDeckDetails(deckId) }
                .onSuccess { deck ->
                    _uiState.update { it.copy(isLoading = false, deck = deck) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun cloneDeck() {
        if (_uiState.value.isCloning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloning = true) }
            runCatching { repository.cloneDeck(deckId) }
                .onSuccess {
                    _uiState.update { it.copy(isCloning = false) }
                    _uiEvent.emit(DetailUiEvent.ShowSnackbar("Talia została sklonowana!"))
                    _uiEvent.emit(DetailUiEvent.NavigateToLocalDecks)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCloning = false) }
                    _uiEvent.emit(DetailUiEvent.ShowSnackbar("Błąd klonowania: ${e.message}"))
                }
        }
    }

    fun reportDeck(reason: String? = null) {
        if (_uiState.value.isReporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReporting = true) }
            runCatching { repository.reportDeck(deckId, reason) }
                .onSuccess {
                    _uiState.update { it.copy(isReporting = false) }
                    _uiEvent.emit(DetailUiEvent.ShowSnackbar("Zgłoszenie zostało wysłane."))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isReporting = false) }
                    _uiEvent.emit(DetailUiEvent.ShowSnackbar("Błąd zgłaszania: ${e.message}"))
                }
        }
    }
}