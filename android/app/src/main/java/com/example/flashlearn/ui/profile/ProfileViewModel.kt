package com.example.flashlearn.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Done : LogoutState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val email: String? = repository.getEmail()
    val registeredAt: String? = repository.getRegisteredAt()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Loading
            repository.logout()      // POST /auth/logout + clearTokens wewnątrz
            _logoutState.value = LogoutState.Done
        }
    }
}
