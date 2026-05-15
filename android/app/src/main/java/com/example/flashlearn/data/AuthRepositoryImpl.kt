package com.example.flashlearn.data

import com.example.flashlearn.data.local.TokenManager
import com.example.flashlearn.data.remote.AuthApiService
import com.example.flashlearn.data.remote.LoginRequest
import com.example.flashlearn.data.remote.LoginResponse
import com.example.flashlearn.data.remote.LogoutRequest
import com.example.flashlearn.data.remote.RegisterRequest
import com.example.flashlearn.data.remote.RegisterResponse
import com.example.flashlearn.domain.repository.AuthRepository
import com.flashlearn.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val db: AppDatabase
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                TokenManager.saveTokens(body.accessToken, body.refreshToken)
                // Zapisz email i datę pierwszego logowania (jeśli nie istnieje)
                if (TokenManager.getEmail() == null ||
                    TokenManager.getEmail() != email
                ) {
                    val now = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    TokenManager.saveEmail(email)
                    TokenManager.saveRegisteredAt(now)
                }
                Result.success(body)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        val refreshToken = TokenManager.getRefreshToken() ?: ""
        clearTokens()
        withContext(Dispatchers.IO) {
            db.clearAllTables()
        }
        return try {
            api.logout(LogoutRequest(refreshToken))
            Result.success(Unit)
        } catch (e: Exception) {
            // Niezależnie od wyniku serwera tokeny są już wyczyszczone
            Result.success(Unit)
        }
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        TokenManager.saveTokens(accessToken, refreshToken)
    }

    override fun clearTokens() {
        TokenManager.clearTokens()
    }

    override fun getAccessToken(): String? = TokenManager.getAccessToken()

    override fun getRefreshToken(): String? = TokenManager.getRefreshToken()

    override fun saveUserInfo(email: String, registeredAt: String) {
        TokenManager.saveEmail(email)
        TokenManager.saveRegisteredAt(registeredAt)
    }

    override fun getEmail(): String? = TokenManager.getEmail()

    override fun getRegisteredAt(): String? = TokenManager.getRegisteredAt()
}
