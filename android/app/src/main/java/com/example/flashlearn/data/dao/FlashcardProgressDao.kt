package com.flashlearn.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flashlearn.data.entity.FlashcardProgress
import kotlinx.coroutines.flow.Flow

/**
 * DAO dla encji [FlashcardProgress] (stan SM-2 fiszek).
 */
@Dao
interface FlashcardProgressDao {

    // ── Odczyt ───────────────────────────────────────────────────────────────

    /** Zwraca postęp dla konkretnej fiszki lub null, gdy jeszcze nie istnieje. */
    @Query("SELECT * FROM flashcard_progress WHERE flashcard_id = :flashcardId LIMIT 1")
    suspend fun getByFlashcard(flashcardId: Long): FlashcardProgress?

    /**
     * Obserwuje fiszki z danej talii, których data powtórki ≤ [todayEpochDay].
     * Wynik sortowany wg daty (najpilniejsze pierwsze).
     */
    @Query(
        """
        SELECT fp.* FROM flashcard_progress fp
        INNER JOIN flashcards f ON f.id = fp.flashcard_id
        WHERE f.deck_id = :deckId
          AND fp.next_review_date <= :todayEpochDay
        ORDER BY fp.next_review_date ASC
        """
    )
    fun observeDue(deckId: Long, todayEpochDay: Long): Flow<List<FlashcardProgress>>

    /**
     * Zwraca liczbę fiszek z danej talii gotowych do powtórki dziś lub wcześniej.
     */
    @Query(
        """
        SELECT COUNT(*) FROM flashcards f
        LEFT JOIN flashcard_progress fp ON f.id = fp.flashcard_id
        WHERE f.deck_id = :deckId
          AND (fp.next_review_date IS NULL OR fp.next_review_date <= :todayEpochDay)
        """
    )
    fun observeDueCount(deckId: Long, todayEpochDay: Long): Flow<Int>

    /**
     * Obserwuje wszystkie postępy dla danej talii, używane m.in. do obliczania statystyk (np. % opanowania).
     */
    @Query(
        """
        SELECT fp.* FROM flashcard_progress fp
        INNER JOIN flashcards f ON f.id = fp.flashcard_id
        WHERE f.deck_id = :deckId
        """
    )
    fun observeProgressByDeck(deckId: Long): Flow<List<FlashcardProgress>>

    // ── Zapis ────────────────────────────────────────────────────────────────

    /**
     * Wstawia nowy rekord postępu.
     * Jeśli rekord dla tej fiszki już istnieje – zastępuje go (używane przy sync).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: FlashcardProgress): Long

    // ── Aktualizacja ─────────────────────────────────────────────────────────

    /** Aktualizuje rekord postępu na podstawie klucza głównego. */
    @Update
    suspend fun update(progress: FlashcardProgress): Int

    // ── Usuwanie ─────────────────────────────────────────────────────────────

    /** Usuwa postęp dla konkretnej fiszki. */
    @Query("DELETE FROM flashcard_progress WHERE flashcard_id = :flashcardId")
    suspend fun deleteByFlashcard(flashcardId: Long): Int

    /** Usuwa wszystkie rekordy (używane przy wylogowaniu). */
    @Query("DELETE FROM flashcard_progress")
    suspend fun deleteAll(): Int

    /**
     * Zwraca true jeśli użytkownik uczył się dziś -
     * tzn. istnieje fiszka której next_review_date > dzisiaj
     * (została już powtórzona i zaplanowana na przyszłość)
     */
    @Query(
        """
    SELECT COUNT(*) > 0 FROM flashcard_progress
    WHERE next_review_date > :todayEpochDay
    AND repetitions > 0
    """
    )
    suspend fun hasStudySessionToday(todayEpochDay: Long): Boolean
}
