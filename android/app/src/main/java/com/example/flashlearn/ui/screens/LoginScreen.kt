package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.LoginRequest
import com.example.flashlearn.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "Email jest wymagany"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Nieprawidłowy format email"
            else -> null
        }
        return emailError == null
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> "Hasło jest wymagane"
            else -> null
        }
        return passwordError == null
    }

    fun login() {
        apiError = null
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()

        if (isEmailValid && isPasswordValid) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.authApi.login(LoginRequest(email, password))
                    TokenManager.saveTokens(response.accessToken, response.refreshToken)
                    isLoading = false
                    onLoginSuccess()
                } catch (e: HttpException) {
                    isLoading = false
                    apiError = when (e.code()) {
                        401 -> "Nieprawidłowy email lub hasło"
                        404 -> "Konto nie istnieje"
                        else -> "Błąd serwera: ${e.code()}"
                    }
                } catch (e: Exception) {
                    isLoading = false
                    apiError = "Błąd połączenia z serwerem"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Logowanie",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) validateEmail()
            },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) validatePassword()
            },
            label = { Text("Hasło") },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (apiError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = apiError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { login() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Zaloguj się")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Nie masz konta? Zarejestruj się")
        }
    }
}
