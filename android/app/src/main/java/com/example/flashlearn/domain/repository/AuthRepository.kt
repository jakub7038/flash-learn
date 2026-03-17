package com.example.flashlearn.domain.repository

import com.example.flashlearn.data.remote.LoginResponse
import com.example.flashlearn.data.remote.RegisterResponse

interface AuthRepository {

    suspend fun login(email: String, password: String): Result<LoginResponse>

    suspend fun register(email: String, password: String): Result<RegisterResponse>

    suspend fun logout(): Result<Unit>

    fun saveTokens(accessToken: String, refreshToken: String)

    fun clearTokens()

    fun getAccessToken(): String?
}