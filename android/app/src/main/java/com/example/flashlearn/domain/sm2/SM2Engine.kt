package com.example.flashlearn.domain.sm2

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Wynik obliczenia algorytmu SM-2.
 *
 * @param easeFactor    nowy współczynnik łatwości (EF ≥ 1.3)
 * @param intervalDays  liczba dni do następnej powtórki
 * @param repetitions   zaktualizowana liczba udanych powtórek z rzędu
 * @param nextReviewDate data następnej zaplanowanej powtórki
 */
data class SM2Result(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val nextReviewDate: LocalDate,
)

/**
 * Silnik algorytmu SM-2 (SuperMemo 2).
 *
 * Skala ocen (0–3):
 * - **0** „Nie wiem"  — odpowiedź błędna / brak odpowiedzi
 * - **1** „Trudne"    — odpowiedź poprawna, ale z poważną trudnością
 * - **2** „Dobre"     — odpowiedź poprawna z pewnym wahaniem
 * - **3** „Łatwe"     — odpowiedź natychmiastowa i pewna
 *
 * Logika:
 * - Ocena 0 → resetuje powtórki (repetitions = 0) i skraca interwał do 1 dnia.
 * - Oceny 1–3 → inkrementują licznik powtórek i przeliczają EF oraz interwał
 *   zgodnie ze wzorem SM-2.
 *
 * @see <a href="https://www.supermemo.com/en/archives1990-2015/english/ol/sm2">SM-2 Algorithm</a>
 */
object SM2Engine {

    /** Minimalna wartość współczynnika łatwości dopuszczona przez algorytm. */
    const val MIN_EASE_FACTOR = 1.3

    /** Domyślna wartość EF dla nowej fiszki. */
    const val DEFAULT_EASE_FACTOR = 2.5

    /**
     * Przelicza stan fiszki po udzieleniu odpowiedzi.
     *
     * @param grade         ocena użytkownika w skali 0–3
     * @param easeFactor    aktualny EF fiszki (domyślnie [DEFAULT_EASE_FACTOR])
     * @param intervalDays  aktualny interwał w dniach
     * @param repetitions   dotychczasowa liczba udanych powtórek z rzędu
     * @param today         data referencencyjna (wstrzykiwana w testach)
     * @return              [SM2Result] z nowym stanem fiszki
     * @throws IllegalArgumentException gdy [grade] jest poza zakresem 0–3
     */
    fun calculate(
        grade: Int,
        easeFactor: Double = DEFAULT_EASE_FACTOR,
        intervalDays: Int = 1,
        repetitions: Int = 0,
        today: LocalDate = LocalDate.now(),
    ): SM2Result {
        require(grade in 0..3) { "Ocena musi być z zakresu 0–3, otrzymano: $grade" }

        // Mapowanie skali 0–3 → skala jakości 0–5 używana w oryginalnym SM-2
        val quality = when (grade) {
            0 -> 0  // kompletna porażka
            1 -> 3  // poprawna z poważną trudnością
            2 -> 4  // poprawna z małym wahaniem
            3 -> 5  // idealna odpowiedź
            else -> throw IllegalArgumentException("grade=$grade")
        }

        val isCorrect = quality >= 3  // grade 1, 2, 3 są odpowiedziami poprawnymi

        // Aktualizacja EF (wzór SM-2, tylko dla odpowiedzi poprawnych)
        val newEF = if (isCorrect) {
            val delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
            (easeFactor + delta).coerceAtLeast(MIN_EASE_FACTOR)
        } else {
            easeFactor.coerceAtLeast(MIN_EASE_FACTOR)
        }

        return if (!isCorrect) {
            // Reset po błędnej odpowiedzi
            SM2Result(
                easeFactor = newEF,
                intervalDays = 1,
                repetitions = 0,
                nextReviewDate = today.plusDays(1),
            )
        } else {
            val newReps = repetitions + 1
            val newInterval = when (newReps) {
                1 -> 1
                2 -> 6
                else -> (intervalDays * newEF).roundToInt().coerceAtLeast(1)
            }
            SM2Result(
                easeFactor = newEF,
                intervalDays = newInterval,
                repetitions = newReps,
                nextReviewDate = today.plusDays(newInterval.toLong()),
            )
        }
    }
}
