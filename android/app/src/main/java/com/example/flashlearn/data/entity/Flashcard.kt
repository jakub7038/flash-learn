package com.flashlearn.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Lokalna encja fiszki – mapuje tabelę `flashcards` w SQLite.
 *
 * Fiszka jest zawsze przypisana do konkretnej talii ([deckId]).
 * Klucz obcy z opcją CASCADE zapewnia automatyczne usunięcie
 * wszystkich fiszek po usunięciu talii nadrzędnej.
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["deck_id"]),
        Index(value = ["server_id"], unique = true),
    ],
)
data class Flashcard(

    /** Lokalny klucz główny generowany przez Room (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Identyfikator po stronie serwera.
     * Null, gdy fiszka jeszcze nie trafiła do backendu.
     */
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    /** Klucz obcy do lokalnej talii [Deck.id]. */
    @ColumnInfo(name = "deck_id")
    val deckId: Long,

    /** Treść pytania wyświetlanego podczas sesji nauki. */
    val question: String,

    /** Poprawna odpowiedź na pytanie. */
    val answer: String,

    // ── Metadane ─────────────────────────────────────────────────────────────

    @ColumnInfo(name = "created_at")
    val createdAt: Long = Instant.now().epochSecond,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = Instant.now().epochSecond,

    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,
)
