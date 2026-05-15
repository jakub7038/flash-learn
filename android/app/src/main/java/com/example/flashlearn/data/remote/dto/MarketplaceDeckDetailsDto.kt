package com.example.flashlearn.data.remote.dto

data class MarketplaceDeckDetailsDto(
    val id: Long,
    val title: String,
    val description: String?,
    val ownerEmail: String,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIconName: String?,
    val flashcards: List<FlashcardDto>,
    val downloadCount: Long,
    val createdAt: String?
)

data class FlashcardDto(
    val id: Long,
    val question: String,
    val answer: String
)