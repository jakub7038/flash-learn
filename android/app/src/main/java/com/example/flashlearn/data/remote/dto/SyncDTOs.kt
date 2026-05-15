package com.example.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SyncPushRequest(
    val clientTimestamp: String,
    val decks: List<SyncDeckDTO>,
    val flashcards: List<SyncFlashcardDTO>
)

data class SyncPushResponse(
    val decksProcessed: Int,
    val flashcardsProcessed: Int,
    val conflicts: List<String>,
    val deckIdMapping: Map<Long, Long>?,
    val flashcardIdMapping: Map<Long, Long>?,
    val serverTimestamp: String
)

data class SyncPullResponse(
    val decks: List<SyncDeckDTO>,
    val flashcards: List<SyncFlashcardDTO>,
    val serverTimestamp: String,
    val page: Int,
    val pageSize: Int,
    val totalDecks: Long,
    val totalFlashcards: Long,
    val hasMore: Boolean
)

data class SyncDeckDTO(
    val id: Long?,
    val localId: Long? = null,
    val title: String,
    val description: String?,
    @SerializedName("public") val isPublic: Boolean,
    val categorySlug: String? = null,
    val updatedAt: String,
    val flashcards: List<SyncFlashcardDTO>? = null
)

data class SyncFlashcardDTO(
    val id: Long?,
    val localId: Long? = null,
    val deckId: Long?,
    val question: String,
    val answer: String,
    val updatedAt: String
)
