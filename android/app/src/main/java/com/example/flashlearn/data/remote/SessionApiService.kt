package com.example.flashlearn.data.remote

import com.example.flashlearn.data.remote.dto.SessionRequest
import com.example.flashlearn.data.remote.dto.SessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SessionApiService {
    @POST("/sessions")
    suspend fun saveSession(@Body request: SessionRequest): Response<SessionResponse>
}
