package com.example.flashlearn.data.remote

import com.example.flashlearn.data.remote.dto.StatsDto
import retrofit2.Response
import retrofit2.http.GET

interface StatsApiService {
    @GET("/stats")
    suspend fun getStats(): Response<StatsDto>
}
