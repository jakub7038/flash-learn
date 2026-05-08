package com.flashlearn.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Lokalna encja talii fiszek – mapuje tabelę `decks` w SQLite.
 *
 * Kolumny są spójne z modelem serwera (PostgreSQL), co upraszcza
 * synchronizację: pole [serverId] przechowuje identyfikator z backendu
 * lub null, gdy talia jeszcze nie została zsynchronizowana.
 */
@Entity(tableName = "decks")
data class Deck(

    /** Lokalny klucz główny generowany przez Room (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Identyfikator po stronie serwera (PostgreSQL BIGSERIAL).
     * Wartość null oznacza, że talia jest lokalna i oczekuje na sync.
     */
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    /** Tytuł talii wyświetlany użytkownikowi. */
    val title: String,

    /** Opcjonalny opis talii. */
    val description: String? = null,

    /** Czy talia jest widoczna w publicznym Marketplace. */
    @ColumnInfo(name = "is_public")
    val isPublic: Boolean = false,

    /** Identyfikator właściciela (odpowiada users.server_id). */
    @ColumnInfo(name = "owner_id")
    val ownerId: Long? = null,

    /** Znacznik czasu utworzenia (epoch-seconds UTC). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = Instant.now().epochSecond,

    /** Znacznik czasu ostatniej modyfikacji (epoch-seconds UTC). */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = Instant.now().epochSecond,

    /**
     * Flaga określająca, czy rekord wymaga synchronizacji z serwerem.
     * Ustawiana na true przy każdej lokalnej zmianie, zerowana po
     * potwierdzeniu synchronizacji przez backend.
     */
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,
    /**
     * Flaga gotowego zestawu readonly (seed z prepopulate).
     * true = zestaw wbudowany, użytkownik nie może go edytować ani usunąć.
     */
    @ColumnInfo(name = "is_readonly")
    val isReadonly: Boolean = false,

    /**
     * Slug kategorii zgodny z FL-143 (jezyki, programowanie, matematyka,
     * nauki-scisle, historia, inne).
     * Null dla talii bez kategorii lub zsynchronizowanych przez server_id.
     */
    @ColumnInfo(name = "category_slug")
    val categorySlug: String? = null,
)
