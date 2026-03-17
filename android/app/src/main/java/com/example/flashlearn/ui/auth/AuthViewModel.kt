package com.example.flashlearn.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.domain.repository.AuthRepository
import com.example.flashlearn.domain.usecase.LoginUseCase
import com.example.flashlearn.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            loginUseCase(email, password).fold(
                onSuccess = { response ->
                    repository.saveTokens(response.accessToken, response.refreshToken)
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(handleAuthError(error))
                }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            registerUseCase(email, password).fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(handleAuthError(error))
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            repository.clearTokens()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun handleAuthError(error: Throwable): String {
        return when (error) {
            is java.io.IOException ->
                "Brak połączenia z internetem. Sprawdź swoje połączenie."
            is java.util.concurrent.TimeoutException,
            is kotlinx.coroutines.TimeoutCancellationException ->
                "Przekroczono czas oczekiwania na odpowiedź serwera."
            is retrofit2.HttpException -> when (error.code()) {
                401 -> "Nieprawidłowy e-mail lub hasło."
                403 -> "Nieprawidłowy e-mail lub hasło."
                409 -> "Użytkownik o takim adresie e-mail już istnieje."
                in 500..599 -> "Błąd serwera. Spróbuj ponownie później."
                else -> "Wystąpił błąd sieciowy (${error.code()})."
            }
            else -> error.message ?: "Wystąpił nieznany błąd."
        }
    }
}