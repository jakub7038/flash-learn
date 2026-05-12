package com.example.flashlearn.data.remote.dto

data class SessionRequest(
    val deckId: Long,
    val startedAt: String,
    val finishedAt: String,
    val results: List<SessionResultRequest>
)

data class SessionResultRequest(
    val flashcardId: Long,
    val rating: Int
)

data class SessionResponse(
    val id: Long,
    val deckId: Long,
    val startedAt: String,
    val finishedAt: String,
    val resultsCount: Int
)
