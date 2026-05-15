package com.example.flashlearn.data.remote.dto

data class ReportRequestDto(
    val deckId: Long,
    val reason: String? = null
)