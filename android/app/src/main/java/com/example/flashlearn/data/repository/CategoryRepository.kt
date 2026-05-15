package com.example.flashlearn.data.repository

import com.example.flashlearn.data.remote.CategoryApiService
import com.example.flashlearn.data.remote.dto.CategoryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: CategoryApiService
) {
    private var cachedCategories: List<CategoryDto>? = null

    suspend fun getCategories(): List<CategoryDto> {
        cachedCategories?.let { return it }
        val response = api.getCategories()
        return if (response.isSuccessful) {
            response.body().orEmpty().also { cachedCategories = it }
        } else {
            emptyList()
        }
    }
}
