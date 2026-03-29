package com.example.flashlearn.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "SyncWorker started")
            
            // 1. Fetch pending decks
            val pendingDecks = deckDao.getPendingSync()
            if (pendingDecks.isNotEmpty()) {
                Log.d(TAG, "Found ${pendingDecks.size} pending decks to sync")
                for (deck in pendingDecks) {
                    // TODO: Replace with actual Retrofit API call
                    // e.g. val response = api.createDeck(deck.toDto())
                    // val serverId = response.id
                    val mockServerId = System.currentTimeMillis() // Placeholder
                    val updatedAt = Instant.now().epochSecond
                    
                    deckDao.markSynced(deck.id, mockServerId, updatedAt)
                    Log.d(TAG, "Deck ${deck.id} synced with mock serverId: $mockServerId")
                }
            }
            
            // 2. Fetch pending flashcards
            val pendingFlashcards = flashcardDao.getPendingSync()
            if (pendingFlashcards.isNotEmpty()) {
                Log.d(TAG, "Found ${pendingFlashcards.size} pending flashcards to sync")
                for (flashcard in pendingFlashcards) {
                    // Fiszki wymagają nadrzędnego serverId lokalnej talii
                    val deck = deckDao.getById(flashcard.deckId)
                    if (deck?.serverId != null) {
                        // TODO: Replace with actual API call
                        val mockServerId = System.currentTimeMillis() + 1 // Placeholder
                        val updatedAt = Instant.now().epochSecond
                        
                        flashcardDao.markSynced(flashcard.id, mockServerId, updatedAt)
                        Log.d(TAG, "Flashcard ${flashcard.id} synced with mock serverId: $mockServerId")
                    } else {
                        Log.w(TAG, "Cannot sync flashcard ${flashcard.id} because deck ${flashcard.deckId} does not have serverId yet.")
                        // Może być tu throw rzucające wyjątek i wywołujące catch -> retry(),
                    }
                }
            }

            Log.d(TAG, "SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            Result.retry() // Pozwala WorkManagerowi spróbować ponownie przy kolejnych okazjach
        }
    }

    companion object {
        const val TAG = "SyncWorker"
    }
}
