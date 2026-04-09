package com.example.flashlearn.domain

import com.example.flashlearn.domain.sm2.SM2Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SM2EngineTest {

    private val today = LocalDate.of(2026, 4, 9)

    // ── Ocena 0 – Nie wiem ────────────────────────────────────────────────────

    @Test
    fun `grade 0 resets repetitions to zero`() {
        val result = SM2Engine.calculate(grade = 0, repetitions = 5, today = today)
        assertEquals(0, result.repetitions)
    }

    @Test
    fun `grade 0 sets interval to 1 day`() {
        val result = SM2Engine.calculate(grade = 0, intervalDays = 20, today = today)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun `grade 0 schedules review for tomorrow`() {
        val result = SM2Engine.calculate(grade = 0, today = today)
        assertEquals(today.plusDays(1), result.nextReviewDate)
    }

    @Test
    fun `grade 0 does not decrease EF below minimum`() {
        val result = SM2Engine.calculate(
            grade = 0,
            easeFactor = SM2Engine.MIN_EASE_FACTOR,
            today = today,
        )
        assertTrue(result.easeFactor >= SM2Engine.MIN_EASE_FACTOR)
    }

    // ── Pierwsza powtórka (grade 1–3, repetitions=0 → 1) ─────────────────────

    @Test
    fun `first correct answer sets interval to 1`() {
        listOf(1, 2, 3).forEach { grade ->
            val result = SM2Engine.calculate(grade = grade, repetitions = 0, today = today)
            assertEquals("grade=$grade", 1, result.intervalDays)
            assertEquals("grade=$grade", 1, result.repetitions)
        }
    }

    // ── Druga powtórka (repetitions=1 → 2) ────────────────────────────────────

    @Test
    fun `second correct answer sets interval to 6`() {
        listOf(1, 2, 3).forEach { grade ->
            val result = SM2Engine.calculate(
                grade = grade, repetitions = 1, intervalDays = 1, today = today
            )
            assertEquals("grade=$grade", 6, result.intervalDays)
            assertEquals("grade=$grade", 2, result.repetitions)
        }
    }

    // ── Kolejne powtórki ───────────────────────────────────────────────────────

    @Test
    fun `subsequent correct answers multiply interval by EF`() {
        val ef = 2.5
        val prevInterval = 6
        val result = SM2Engine.calculate(
            grade = 3,
            easeFactor = ef,
            intervalDays = prevInterval,
            repetitions = 2,
            today = today,
        )
        // interval = round(6 * 2.6) = 16 (EF increases for grade 3)
        assertTrue(result.intervalDays > prevInterval)
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `nextReviewDate equals today plus intervalDays`() {
        val result = SM2Engine.calculate(
            grade = 3, repetitions = 2, intervalDays = 6, today = today
        )
        assertEquals(today.plusDays(result.intervalDays.toLong()), result.nextReviewDate)
    }

    // ── EF – współczynnik łatwości ─────────────────────────────────────────────

    @Test
    fun `grade 3 increases EF`() {
        val result = SM2Engine.calculate(
            grade = 3,
            easeFactor = SM2Engine.DEFAULT_EASE_FACTOR,
            repetitions = 1,
            today = today,
        )
        assertTrue(result.easeFactor > SM2Engine.DEFAULT_EASE_FACTOR)
    }

    @Test
    fun `grade 2 keeps EF unchanged`() {
        val result = SM2Engine.calculate(
            grade = 2,
            easeFactor = SM2Engine.DEFAULT_EASE_FACTOR,
            repetitions = 1,
            today = today,
        )
        assertEquals(SM2Engine.DEFAULT_EASE_FACTOR, result.easeFactor, 1e-9)
    }

    @Test
    fun `grade 1 decreases EF but not below minimum`() {
        val result = SM2Engine.calculate(
            grade = 1,
            easeFactor = SM2Engine.DEFAULT_EASE_FACTOR,
            repetitions = 1,
            today = today,
        )
        assertTrue(result.easeFactor < SM2Engine.DEFAULT_EASE_FACTOR)
        assertTrue(result.easeFactor >= SM2Engine.MIN_EASE_FACTOR)
    }

    @Test
    fun `EF never goes below minimum even on repeated hard answers`() {
        var ef = SM2Engine.DEFAULT_EASE_FACTOR
        var interval = 1
        var reps = 0
        repeat(20) {
            val r = SM2Engine.calculate(
                grade = 1, easeFactor = ef, intervalDays = interval,
                repetitions = reps, today = today,
            )
            ef = r.easeFactor
            interval = r.intervalDays
            reps = r.repetitions
        }
        assertTrue(ef >= SM2Engine.MIN_EASE_FACTOR)
    }

    // ── Walidacja ──────────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `grade below 0 throws IllegalArgumentException`() {
        SM2Engine.calculate(grade = -1, today = today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `grade above 3 throws IllegalArgumentException`() {
        SM2Engine.calculate(grade = 4, today = today)
    }
}
