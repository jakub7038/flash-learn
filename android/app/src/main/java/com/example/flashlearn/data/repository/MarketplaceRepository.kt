package com.example.flashlearn.data.repository

import com.example.flashlearn.data.remote.MarketplaceApiService
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDetailsDto
import com.example.flashlearn.data.remote.dto.MarketplaceDeckDto
import com.example.flashlearn.data.remote.dto.ReportRequestDto
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.entity.Deck
import com.flashlearn.data.entity.Flashcard
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepository @Inject constructor(
    private val api: MarketplaceApiService,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao
) {

    /**
     * Zwraca listę publicznych talii, opcjonalnie filtrowaną po kategorii.
     * Backend zawsze sortuje po popularności (downloadCount DESC).
     */
    suspend fun getPublicDecks(category: String? = null): List<MarketplaceDeckDto> {
        val response = api.getDecks(category = category, page = 0)
        if (response.isSuccessful) {
            return response.body()?.decks ?: emptyList()
        }
        error("Marketplace fetch failed: ${response.code()}")
    }

    /**
     * Klonuje talię do biblioteki zalogowanego użytkownika.
     * Po udanym klonowaniu natychmiast zapisuje talię i fiszki do lokalnej bazy Room,
     * dzięki czemu talia pojawia się w "Moje talie" bez konieczności przelogowania.
     */
    suspend fun cloneDeck(deckId: Long) {
        val response = api.cloneDeck(deckId)
        if (!response.isSuccessful) {
            error("Clone failed: ${response.code()}")
        }

        val cloned = response.body() ?: return

        // Zapisz sklonowaną talię do Room z serverId z backendu
        val now = Instant.now().epochSecond
        val localDeckId = deckDao.insert(
            Deck(
                serverId  = cloned.deckId,
                title     = cloned.title,
                description = cloned.description,
                isPublic  = false,
                needsSync = false,   // talia pochodzi z serwera — nie wymaga sync
                createdAt = now,
                updatedAt = now
            )
        )

        // Zapisz fiszki
        val flashcards = cloned.flashcards.map { f ->
            Flashcard(
                serverId  = f.id,
                deckId    = localDeckId,
                question  = f.question,
                answer    = f.answer,
                needsSync = false,
                createdAt = now,
                updatedAt = now
            )
        }
        if (flashcards.isNotEmpty()) {
            flashcardDao.insertAll(flashcards)
        }
    }

    suspend fun getDeckDetails(deckId: Long): MarketplaceDeckDetailsDto {
        val response = api.getDeckDetails(deckId)
        if (response.isSuccessful) {
            return response.body() ?: error("Empty response body")
        }
        error("Failed to fetch deck details: HTTP ${response.code()}")
    }

    suspend fun reportDeck(deckId: Long, reason: String? = null) {
        val response = api.reportDeck(ReportRequestDto(deckId, reason))
        if (!response.isSuccessful) {
            error("Report failed: HTTP ${response.code()}")
        }
    }

}

