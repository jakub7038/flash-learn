package com.example.flashlearn.data.repository

import android.util.Log
import com.example.flashlearn.data.remote.DeckApiService

import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.DeckWithCount
import com.flashlearn.data.entity.Deck
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepository @Inject constructor(
    private val deckDao: DeckDao,
    private val syncManager: com.example.flashlearn.sync.SyncManager,
    private val deckApi: DeckApiService
) {
    fun observeAllDecks(): Flow<List<Deck>> = deckDao.observeAll()

    fun observeAllDecksWithCount(): Flow<List<DeckWithCount>> = deckDao.observeAllWithCount()

    suspend fun getDeckById(id: Long): Deck? = deckDao.getById(id)

    suspend fun createDeck(
        title: String,
        description: String? = null,
        categorySlug: String? = null
    ): Long {
        val deck = Deck(
            title = title,
            description = description,
            categorySlug = categorySlug,
            createdAt = Instant.now().epochSecond,
            updatedAt = Instant.now().epochSecond,
        )
        val id = deckDao.insert(deck)
        syncManager.scheduleSync()
        return id
    }

    suspend fun updateDeck(
        id: Long,
        title: String,
        description: String?,
        categorySlug: String? = null
    ) {
        val existing = deckDao.getById(id) ?: return
        deckDao.update(
            existing.copy(
                title = title,
                description = description,
                categorySlug = categorySlug,
                updatedAt = Instant.now().epochSecond,
                needsSync = true
            )
        )
        syncManager.scheduleSync()
    }

    suspend fun deleteDeck(deck: Deck) {
        if (deck.serverId != null) {
            try {
                deckApi.deleteDeck(deck.serverId)
            } catch (e: Exception) {
                Log.e("DeckRepository", "Failed to delete deck from server", e)
            }
        }
        deckDao.delete(deck)
        syncManager.scheduleSync()
    }

    suspend fun deleteDeckById(id: Long) {
        val deck = deckDao.getById(id)
        if (deck != null && deck.serverId != null) {
            try {
                deckApi.deleteDeck(deck.serverId)
            } catch (e: Exception) {
                Log.e("DeckRepository", "Failed to delete deck from server", e)
            }
        }
        deckDao.deleteById(id)
        syncManager.scheduleSync()
    }
}
