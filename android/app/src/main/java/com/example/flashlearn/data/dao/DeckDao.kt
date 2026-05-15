package com.flashlearn.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flashlearn.data.entity.Deck
import kotlinx.coroutines.flow.Flow

data class DeckWithCount(
    val id: Long,
    val title: String,
    val description: String?,
    val updatedAt: Long,
    val categorySlug: String?,
    val flashcardCount: Int
)

/**
 * DAO (Data Access Object) dla encji [Deck].
 *
 * Wszystkie operacje zapisu zwracają wyniki pozwalające stwierdzić
 * powodzenie operacji.
 */
@Dao
interface DeckDao {

    // ── Odczyt ───────────────────────────────────────────────────────────────

    /**
     * Obserwuje wszystkie talie posortowane od najnowszej, wraz z liczbą fiszek.
     */
    @Query("""
        SELECT d.id, d.title, d.description, d.updated_at AS updatedAt,
               d.category_slug AS categorySlug, COUNT(f.id) AS flashcardCount
        FROM decks d
        LEFT JOIN flashcards f ON d.id = f.deck_id
        GROUP BY d.id
        ORDER BY d.created_at DESC
    """)
    fun observeAllWithCount(): Flow<List<DeckWithCount>>

    /**
     * Obserwuje wszystkie talie posortowane od najnowszej.
     * Emituje nową listę przy każdej zmianie w tabeli.
     */
    @Query("SELECT * FROM decks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<Deck>>

    /** Jednorazowy odczyt wszystkich talii (przydatny w testach). */
    @Query("SELECT * FROM decks ORDER BY created_at DESC")
    suspend fun getAll(): List<Deck>

    /** Zwraca talię o podanym lokalnym identyfikatorze lub null. */
    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Deck?

    /** Zwraca talię powiązaną z identyfikatorem serwera lub null. */
    @Query("SELECT * FROM decks WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): Deck?

    /**
     * Zwraca wszystkie talie oznaczone jako publiczne
     * (widoczne w Marketplace innych użytkowników).
     */
    @Query("SELECT * FROM decks WHERE is_public = 1 ORDER BY created_at DESC")
    fun observePublic(): Flow<List<Deck>>

    /**
     * Zwraca talie oczekujące na synchronizację z serwerem.
     * Używane przez moduł synchronizacji w tle.
     */
    @Query("SELECT * FROM decks WHERE needs_sync = 1")
    suspend fun getPendingSync(): List<Deck>

    // ── Zapis ────────────────────────────────────────────────────────────────

    /**
     * Wstawia nową talię.
     * Strategia ABORT – wyjątek przy próbie duplikacji klucza głównego.
     *
     * @return lokalny id wstawionego rekordu.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: Deck): Long

    /**
     * Wstawia lub zastępuje wiele talii naraz (używane podczas sync
     * – dane z serwera mogą nadpisywać lokalną wersję).
     *
     * @return lista lokalnych id wstawionych/zaktualizowanych rekordów.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(decks: List<Deck>): List<Long>

    // ── Aktualizacja ─────────────────────────────────────────────────────────

    /**
     * Aktualizuje istniejącą talię na podstawie klucza głównego.
     *
     * @return liczba zaktualizowanych rekordów (0 lub 1).
     */
    @Update
    suspend fun update(deck: Deck): Int

    /**
     * Oznacza talię jako zsynchronizowaną i zapisuje jej server_id.
     * Wywoływane po potwierdzeniu operacji przez backend.
     */
    @Query(
        """
        UPDATE decks
        SET server_id  = :serverId,
            needs_sync = 0,
            updated_at = :updatedAt
        WHERE id = :localId
        """
    )
    suspend fun markSynced(localId: Long, serverId: Long, updatedAt: Long): Int

    // ── Usuwanie ─────────────────────────────────────────────────────────────

    /**
     * Usuwa konkretną talię. Kaskada w [Flashcard] usuwa też wszystkie
     * powiązane fiszki.
     *
     * @return liczba usuniętych rekordów.
     */
    @Delete
    suspend fun delete(deck: Deck): Int

    /** Usuwa wszystkie talie z bazy (przydatne w testach i przy wylogowaniu). */
    @Query("DELETE FROM decks")
    suspend fun deleteAll(): Int

    /** Usuwa talię po ID. */
    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Obserwuje gotowe zestawy readonly (seed z prepopulate).
     * Wyświetlane w sekcji "Gotowe zestawy" na liście talii.
     */
    @Query("""
    SELECT d.id, d.title, d.description, d.updated_at AS updatedAt,
           d.category_slug AS categorySlug, COUNT(f.id) AS flashcardCount
    FROM decks d
    LEFT JOIN flashcards f ON d.id = f.deck_id
    WHERE d.is_readonly = 1
    GROUP BY d.id
    ORDER BY d.id ASC
""")
    fun observeReadonlyWithCount(): Flow<List<DeckWithCount>>

    /**
     * Obserwuje talie użytkownika (nie-readonly) posortowane od najnowszej.
     */
    @Query("""
    SELECT d.id, d.title, d.description, d.updated_at AS updatedAt,
           d.category_slug AS categorySlug, COUNT(f.id) AS flashcardCount
    FROM decks d
    LEFT JOIN flashcards f ON d.id = f.deck_id
    WHERE d.is_readonly = 0
    GROUP BY d.id
    ORDER BY d.created_at DESC
""")
    fun observeUserDecksWithCount(): Flow<List<DeckWithCount>>
}
