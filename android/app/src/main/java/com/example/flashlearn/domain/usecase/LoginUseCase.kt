package com.example.flashlearn.domain.usecase

import com.example.flashlearn.data.remote.LoginResponse
import com.example.flashlearn.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<LoginResponse> {
        if (email.isBlank() || !email.contains("@"))
            return Result.failure(Exception("Nieprawidłowy email"))
        if (password.length < 6)
            return Result.failure(Exception("Hasło musi mieć min. 6 znaków"))

        return repository.login(email, password)
    }
}