package com.example.flashlearn.data.repository

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
    private val syncManager: com.example.flashlearn.sync.SyncManager
) {
    fun observeAllDecks(): Flow<List<Deck>> = deckDao.observeAll()

    fun observeAllDecksWithCount(): Flow<List<DeckWithCount>> = deckDao.observeAllWithCount()

    suspend fun createDeck(title: String, description: String? = null): Long {
        val deck = Deck(
            title = title,
            description = description,
            createdAt = Instant.now().epochSecond,
            updatedAt = Instant.now().epochSecond,
        )
        val id = deckDao.insert(deck)
        syncManager.scheduleSync()
        return id
    }

    suspend fun deleteDeck(deck: Deck) {
        deckDao.delete(deck)
        syncManager.scheduleSync()
    }

    suspend fun deleteDeckById(id: Long) {
        deckDao.deleteById(id)
        syncManager.scheduleSync()
    }
}
