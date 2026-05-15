package com.example.flashlearn.data.remote

import com.example.flashlearn.data.remote.dto.CategoryDto
import retrofit2.Response
import retrofit2.http.GET

interface CategoryApiService {
    @GET("/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>
}
