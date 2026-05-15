package com.example.flashlearn.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flashlearn.data.remote.SyncApiService
import com.example.flashlearn.data.remote.dto.SyncDeckDTO
import com.example.flashlearn.data.remote.dto.SyncFlashcardDTO
import com.example.flashlearn.data.remote.dto.SyncPushRequest
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.entity.Deck
import com.flashlearn.data.entity.Flashcard
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val api: SyncApiService,
    private val prefs: SharedPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "SyncWorker started")
            
            val lastSync = prefs.getString("last_sync_timestamp", "1970-01-01T00:00:00")!!
            val currentTimestampStr = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

            // 1. Fetch pending decks
            val pendingDecks = deckDao.getPendingSync()
            val pendingFlashcards = flashcardDao.getPendingSync()
            
            if (pendingDecks.isNotEmpty() || pendingFlashcards.isNotEmpty()) {
                val deckDtos = pendingDecks.map {
                    SyncDeckDTO(
                        id = it.serverId,
                        localId = it.id,
                        title = it.title,
                        description = it.description,
                        isPublic = it.isPublic,
                        categorySlug = it.categorySlug,
                        updatedAt = Instant.ofEpochSecond(it.updatedAt).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                }
                
                val flashcardDtos = pendingFlashcards.mapNotNull { flashcard ->
                    val deck = deckDao.getById(flashcard.deckId)
                    if (deck?.serverId != null) {
                        SyncFlashcardDTO(
                            id = flashcard.serverId,
                            localId = flashcard.id,
                            deckId = deck.serverId,
                            question = flashcard.question,
                            answer = flashcard.answer,
                            updatedAt = Instant.ofEpochSecond(flashcard.updatedAt).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
                        )
                    } else {
                        Log.w(TAG, "Cannot sync flashcard ${flashcard.id} because deck ${flashcard.deckId} does not have serverId yet.")
                        null
                    }
                }
                
                val pushRequest = SyncPushRequest(
                    clientTimestamp = currentTimestampStr,
                    decks = deckDtos,
                    flashcards = flashcardDtos
                )
                
                Log.d(TAG, "Pushing \${deckDtos.size} decks and \${flashcardDtos.size} flashcards")
                val pushResponse = api.push(pushRequest)
                
                // Zapisz wygenerowane ID z serwera
                pushResponse.deckIdMapping?.forEach { (localId, serverId) ->
                    deckDao.markSynced(localId, serverId, Instant.now().epochSecond)
                }
                pushResponse.flashcardIdMapping?.forEach { (localId, serverId) ->
                    flashcardDao.markSynced(localId, serverId, Instant.now().epochSecond)
                }
                
                // Uzupełnij aktualizacje tych, z id ale po prostu zmodyfikowanych
                pendingDecks.filter { it.serverId != null }.forEach {
                    deckDao.markSynced(it.id, it.serverId!!, Instant.now().epochSecond)
                }
                pendingFlashcards.filter { it.serverId != null }.forEach {
                    flashcardDao.markSynced(it.id, it.serverId!!, Instant.now().epochSecond)
                }
            }

            // 2. Fetch changes from server (Pull)
            Log.d(TAG, "Pulling changes since $lastSync")
            val pullResponse = api.pull(since = lastSync)
            
            pullResponse.decks.forEach { dto ->
                // Parsowanie by chronić przed problemami po obcięciu do samej daty np.
                val ldt = try { LocalDateTime.parse(dto.updatedAt, DateTimeFormatter.ISO_DATE_TIME) } catch (e: Exception) { LocalDateTime.parse(dto.updatedAt) }
                val updatedAtEpoch = ldt.toEpochSecond(ZoneOffset.UTC)
                
                val existingDeck = deckDao.getByServerId(dto.id!!)
                if (existingDeck != null) {
                    deckDao.update(existingDeck.copy(
                        title = dto.title,
                        description = dto.description,
                        isPublic = dto.isPublic,
                        categorySlug = dto.categorySlug,
                        updatedAt = updatedAtEpoch,
                        needsSync = false
                    ))
                } else {
                    deckDao.insert(Deck(
                        serverId = dto.id,
                        title = dto.title,
                        description = dto.description,
                        isPublic = dto.isPublic,
                        categorySlug = dto.categorySlug,
                        updatedAt = updatedAtEpoch,
                        needsSync = false
                    ))
                }
            }
            
            pullResponse.flashcards.forEach { dto ->
                val ldt = try { LocalDateTime.parse(dto.updatedAt, DateTimeFormatter.ISO_DATE_TIME) } catch (e: Exception) { LocalDateTime.parse(dto.updatedAt) }
                val updatedAtEpoch = ldt.toEpochSecond(ZoneOffset.UTC)
                
                val localDeck = deckDao.getByServerId(dto.deckId!!)
                if (localDeck != null) {
                    val existingFlashcard = flashcardDao.getByServerId(dto.id!!)
                    if (existingFlashcard != null) {
                        flashcardDao.update(existingFlashcard.copy(
                            question = dto.question,
                            answer = dto.answer,
                            updatedAt = updatedAtEpoch,
                            needsSync = false
                        ))
                    } else {
                        flashcardDao.insert(Flashcard(
                            serverId = dto.id,
                            deckId = localDeck.id,
                            question = dto.question,
                            answer = dto.answer,
                            updatedAt = updatedAtEpoch,
                            needsSync = false
                        ))
                    }
                }
            }
            
            prefs.edit().putString("last_sync_timestamp", pullResponse.serverTimestamp).apply()
            Log.d(TAG, "SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SyncWorker"
    }
}
