package com.example.flashlearn.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.data.remote.dto.StatsDto
import com.example.flashlearn.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val stats: StatsDto) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            repository.getStats().collect { result ->
                result.onSuccess { stats ->
                    _uiState.value = StatsUiState.Success(stats)
                }.onFailure { error ->
                    _uiState.value = StatsUiState.Error(error.message ?: "Wystąpił błąd")
                }
            }
        }
    }
}
