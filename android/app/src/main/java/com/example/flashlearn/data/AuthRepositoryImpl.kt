package com.example.flashlearn.data

import android.content.SharedPreferences
import com.example.flashlearn.data.remote.AuthApiService
import com.example.flashlearn.data.remote.LoginRequest
import com.example.flashlearn.data.remote.LoginResponse
import com.example.flashlearn.data.remote.RegisterRequest
import com.example.flashlearn.data.remote.RegisterResponse
import com.example.flashlearn.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val prefs: SharedPreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
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
        clearTokens()
        return try {
            api.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    override fun clearTokens() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .apply()
    }

    override fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }
}