package com.example.flashlearn.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StatsDto(
    @SerializedName("currentStreak") val currentStreak: Int,
    @SerializedName("longestStreak") val longestStreak: Int,
    @SerializedName("cardsPerDayLast7") val cardsPerDayLast7: Map<String, Long>,
    @SerializedName("cardsPerDayLast30") val cardsPerDayLast30: Map<String, Long>,
    @SerializedName("wrongAnswers") val wrongAnswers: Long,
    @SerializedName("hardAnswers") val hardAnswers: Long,
    @SerializedName("correctAnswers") val correctAnswers: Long,
    @SerializedName("totalReviewed") val totalReviewed: Long
)
