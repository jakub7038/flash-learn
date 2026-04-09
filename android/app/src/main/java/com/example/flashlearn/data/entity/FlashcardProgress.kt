package com.flashlearn.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flashlearn.domain.sm2.SM2Engine
import java.time.LocalDate

/**
 * Stan nauki fiszki wg algorytmu SM-2 – przechowywany lokalnie w SQLite.
 *
 * Jeden rekord odpowiada jednemu unikalnej fiszce ([flashcardId]).
 * Usunięcie fiszki kaskadowo usuwa powiązany rekord postępu (CASCADE).
 *
 * Data [nextReviewEpochDay] przechowywana jest jako liczba dni od epoki
 * (1970-01-01) – odpowiednik [LocalDate.toEpochDay()].
 */
@Entity(
    tableName = "flashcard_progress",
    foreignKeys = [
        ForeignKey(
            entity = Flashcard::class,
            parentColumns = ["id"],
            childColumns = ["flashcard_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["flashcard_id"], unique = true),
    ],
)
data class FlashcardProgress(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Klucz obcy do lokalnej fiszki [Flashcard.id]. */
    @ColumnInfo(name = "flashcard_id")
    val flashcardId: Long,

    /**
     * Współczynnik łatwości (Ease Factor).
     * Domyślnie [SM2Engine.DEFAULT_EASE_FACTOR] = 2.5. Minimum: [SM2Engine.MIN_EASE_FACTOR] = 1.3.
     */
    @ColumnInfo(name = "ease_factor")
    val easeFactor: Double = SM2Engine.DEFAULT_EASE_FACTOR,

    /** Aktualny interwał w dniach do następnej powtórki. */
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 1,

    /** Liczba poprawnych odpowiedzi z rzędu; reset do 0 przy ocenie 0. */
    val repetitions: Int = 0,

    /**
     * Data następnej powtórki jako [LocalDate.toEpochDay()].
     * Domyślnie dzisiaj – fiszka jest gotowa do nauki od razu.
     */
    @ColumnInfo(name = "next_review_date")
    val nextReviewEpochDay: Long = LocalDate.now().toEpochDay(),
)
