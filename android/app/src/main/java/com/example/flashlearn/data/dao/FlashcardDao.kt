package com.flashlearn.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) dla encji [Flashcard].
 *
 * Zawiera operacje CRUD
 */
@Dao
interface FlashcardDao {

    // ── Odczyt ───────────────────────────────────────────────────────────────

    /** Obserwuje wszystkie fiszki z danej talii. */
    @Query("SELECT * FROM flashcards WHERE deck_id = :deckId ORDER BY created_at ASC")
    fun observeByDeck(deckId: Long): Flow<List<Flashcard>>

    /** Jednorazowy odczyt wszystkich fiszek z danej talii. */
    @Query("SELECT * FROM flashcards WHERE deck_id = :deckId ORDER BY created_at ASC")
    suspend fun getByDeck(deckId: Long): List<Flashcard>

    /** Zwraca liczbę fiszek w danej talii. */
    @Query("SELECT COUNT(*) FROM flashcards WHERE deck_id = :deckId")
    fun observeCountByDeck(deckId: Long): Flow<Int>

    /** Zwraca fiszkę o podanym lokalnym id lub null. */
    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Flashcard?

    /** Zwraca fiszkę powiązaną z identyfikatorem serwera lub null. */
    @Query("SELECT * FROM flashcards WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): Flashcard?

    /** Zwraca fiszki oczekujące na synchronizację z serwerem. */
    @Query("SELECT * FROM flashcards WHERE needs_sync = 1")
    suspend fun getPendingSync(): List<Flashcard>

    // ── Zapis ────────────────────────────────────────────────────────────────

    /**
     * Wstawia nową fiszkę.
     *
     * @return lokalny id wstawionego rekordu.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(flashcard: Flashcard): Long

    /**
     * Wstawia lub zastępuje wiele fiszek (używane podczas synchronizacji).
     *
     * @return lista lokalnych id wstawionych/zaktualizowanych rekordów.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flashcards: List<Flashcard>): List<Long>

    // ── Aktualizacja ─────────────────────────────────────────────────────────

    /** Aktualizuje fiszkę na podstawie klucza głównego. */
    @Update
    suspend fun update(flashcard: Flashcard): Int

    /**
     * Oznacza fiszkę jako zsynchronizowaną po potwierdzeniu przez serwer.
     */
    @Query(
        """
        UPDATE flashcards
        SET server_id  = :serverId,
            needs_sync = 0,
            updated_at = :updatedAt
        WHERE id = :localId
        """
    )
    suspend fun markSynced(localId: Long, serverId: Long, updatedAt: Long): Int

    // ── Usuwanie ─────────────────────────────────────────────────────────────

    /** Usuwa konkretną fiszkę. */
    @Delete
    suspend fun delete(flashcard: Flashcard): Int

    /** Usuwa wszystkie fiszki z danej talii. */
    @Query("DELETE FROM flashcards WHERE deck_id = :deckId")
    suspend fun deleteByDeck(deckId: Long): Int

    /** Usuwa wszystkie fiszki (używane w testach i przy wylogowaniu). */
    @Query("DELETE FROM flashcards")
    suspend fun deleteAll(): Int
}
