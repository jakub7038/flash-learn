package com.example.flashlearn

import com.example.flashlearn.data.remote.LoginResponse
import com.example.flashlearn.data.remote.RegisterResponse
import com.example.flashlearn.domain.repository.AuthRepository
import com.example.flashlearn.domain.usecase.LoginUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val fakeRepository = object : AuthRepository {
        override suspend fun login(email: String, password: String): Result<LoginResponse> {
            return Result.success(LoginResponse("fake_access", "fake_refresh"))
        }
        override suspend fun register(email: String, password: String): Result<RegisterResponse> =
            Result.failure(Exception())
        override suspend fun logout(): Result<Unit> = Result.success(Unit)
        override fun saveTokens(accessToken: String, refreshToken: String) {}
        override fun clearTokens() {}
        override fun getAccessToken(): String? = null
    }

    private val loginUseCase = LoginUseCase(fakeRepository)

    @Test
    fun `poprawny email i haslo zwraca sukces`() = runTest {
        val result = loginUseCase("test@example.com", "password123")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `zly email zwraca blad`() = runTest {
        val result = loginUseCase("niemail", "password123")
        assertTrue(result.isFailure)
        assertEquals("Nieprawidłowy email", result.exceptionOrNull()?.message)
    }

    @Test
    fun `za krotkie haslo zwraca blad`() = runTest {
        val result = loginUseCase("test@example.com", "123")
        assertTrue(result.isFailure)
        assertEquals("Hasło musi mieć min. 6 znaków", result.exceptionOrNull()?.message)
    }
}