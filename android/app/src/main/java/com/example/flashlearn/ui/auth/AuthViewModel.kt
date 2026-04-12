package com.example.flashlearn.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlearn.R
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
    application: Application,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val repository: AuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            loginUseCase(email, password).fold(
                onSuccess = { response ->
                    repository.saveTokens(response.accessToken, response.refreshToken)
                    
                    val constraints = androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                    val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.flashlearn.sync.SyncWorker>()
                        .setConstraints(constraints)
                        .build()
                    androidx.work.WorkManager.getInstance(getApplication()).enqueue(syncRequest)

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
        val app = getApplication<Application>()
        return when (error) {
            is java.io.IOException ->
                app.getString(R.string.error_no_internet)
            is java.util.concurrent.TimeoutException,
            is kotlinx.coroutines.TimeoutCancellationException ->
                app.getString(R.string.error_timeout)
            is retrofit2.HttpException -> when (error.code()) {
                401 -> app.getString(R.string.error_invalid_credentials)
                403 -> app.getString(R.string.error_invalid_credentials)
                409 -> app.getString(R.string.error_email_already_exists)
                in 500..599 -> app.getString(R.string.error_server)
                else -> app.getString(R.string.error_network_code, error.code())
            }
            else -> error.message ?: app.getString(R.string.error_unknown)
        }
    }
}
