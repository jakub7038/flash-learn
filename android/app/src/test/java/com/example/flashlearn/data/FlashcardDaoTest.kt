package com.flashlearn.data

import androidx.test.core.app.ApplicationProvider
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Deck
import com.flashlearn.data.entity.Flashcard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testy instrumentacyjne dla [FlashcardDao].
 *
 * Każdy test operuje na osobnej bazie in-memory tworzonej w [setup].
 * Przed każdym testem wstawiana jest jedna domyślna talia ([testDeckId]),
 * do której przypisywane są fiszki – zgodnie z ograniczeniem FK.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlashcardDaoTest {

    private lateinit var db: AppDatabase
    private var testDeckId: Long = 0L

    @Before
    fun setup() = runTest {
        db = AppDatabase.buildInMemory(
            ApplicationProvider.getApplicationContext()
        )
        testDeckId = db.deckDao().insert(Deck(title = "Talia testowa"))
    }

    @After
    fun teardown() {
        db.close()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildCard(
        question: String = "Pytanie?",
        answer: String = "Odpowiedź",
        deckId: Long = testDeckId,
    ) = Flashcard(
        deckId = deckId,
        question = question,
        answer = answer,
    )

    // ── INSERT ────────────────────────────────────────────────────────────────

    @Test
    fun `insert returns valid local id`() = runTest {
        val id = db.flashcardDao().insert(buildCard())
        assertTrue(id > 0)
    }

    @Test
    fun `inserted card can be retrieved by id`() = runTest {
        val id = db.flashcardDao().insert(buildCard(question = "Co to jest Room?"))
        val card = db.flashcardDao().getById(id)

        assertNotNull(card)
        assertEquals("Co to jest Room?", card!!.question)
    }

    @Test
    fun `insertAll inserts multiple cards`() = runTest {
        db.flashcardDao().insertAll(
            listOf(
                buildCard("Q1", "A1"),
                buildCard("Q2", "A2"),
                buildCard("Q3", "A3"),
            )
        )
        assertEquals(3, db.flashcardDao().getByDeck(testDeckId).size)
    }

    // ── SELECT ────────────────────────────────────────────────────────────────

    @Test
    fun `getById returns null for non-existent id`() = runTest {
        assertNull(db.flashcardDao().getById(999L))
    }

    @Test
    fun `observeByDeck emits only cards from the given deck`() = runTest {
        val otherDeckId = db.deckDao().insert(Deck(title = "Inna talia"))
        db.flashcardDao().insert(buildCard("Moje", deckId = testDeckId))
        db.flashcardDao().insert(buildCard("Nie moje", deckId = otherDeckId))

        val cards = db.flashcardDao().observeByDeck(testDeckId).first()
        assertEquals(1, cards.size)
        assertEquals("Moje", cards.first().question)
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    fun `update changes answer`() = runTest {
        val id = db.flashcardDao().insert(buildCard(answer = "Stara"))
        val card = db.flashcardDao().getById(id)!!

        db.flashcardDao().update(card.copy(answer = "Nowa"))

        assertEquals("Nowa", db.flashcardDao().getById(id)!!.answer)
    }

    @Test
    fun `markSynced clears needsSync flag and sets serverId`() = runTest {
        val id = db.flashcardDao().insert(buildCard())

        db.flashcardDao().markSynced(id, serverId = 7L, updatedAt = 0L)

        val synced = db.flashcardDao().getById(id)!!
        assertEquals(7L, synced.serverId)
        assertEquals(false, synced.needsSync)
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    fun `delete removes specific card`() = runTest {
        val id = db.flashcardDao().insert(buildCard())
        val card = db.flashcardDao().getById(id)!!

        db.flashcardDao().delete(card)

        assertNull(db.flashcardDao().getById(id))
    }

    @Test
    fun `deleteByDeck removes all cards from given deck`() = runTest {
        repeat(5) { db.flashcardDao().insert(buildCard()) }
        db.flashcardDao().deleteByDeck(testDeckId)

        assertTrue(db.flashcardDao().getByDeck(testDeckId).isEmpty())
    }

    @Test
    fun `deleting parent deck cascades to flashcards`() = runTest {
        val deck = db.deckDao().getById(testDeckId)!!
        db.flashcardDao().insert(buildCard())

        db.deckDao().delete(deck)

        assertTrue(db.flashcardDao().getByDeck(testDeckId).isEmpty())
    }
}
