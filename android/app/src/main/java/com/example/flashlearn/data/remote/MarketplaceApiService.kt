package com.example.flashlearn.data.remote

import com.example.flashlearn.data.remote.dto.CloneResponseDto
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDetailsDto
import com.example.flashlearn.data.remote.dto.MarketplacePageDto
import com.example.flashlearn.data.remote.dto.ReportRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MarketplaceApiService {

    /**
     * GET /marketplace
     * Zwraca stronicowaną listę publicznych talii.
     * Sortowanie po popularności obsługiwane przez backend.
     *
     * @param category  slug kategorii (np. "jezyki"), null = wszystkie
     * @param page      numer strony (0-based, domyślnie 0)
     */
    @GET("/marketplace")
    suspend fun getDecks(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 0
    ): Response<MarketplacePageDto>

    /**
     * POST /marketplace/{id}/clone
     * Klonuje wskazaną talię do biblioteki zalogowanego użytkownika.
     * Odpowiedź: 201 Created z CloneResponse.
     */
    @POST("/marketplace/{id}/clone")
    suspend fun cloneDeck(@Path("id") deckId: Long): Response<CloneResponseDto>

    @GET("/marketplace/{id}")
    suspend fun getDeckDetails(@Path("id") deckId: Long): Response<MarketplaceDeckDetailsDto>

    @POST("/marketplace/report")
    suspend fun reportDeck(@Body request: ReportRequestDto): Response<Void>

}

