package com.flashlearn.data

import androidx.test.core.app.ApplicationProvider
import com.flashlearn.data.db.AppDatabase
import com.flashlearn.data.entity.Deck
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
 * Testy instrumentacyjne dla [DeckDao].
 *
 * Każdy test operuje na świeżej bazie in-memory – dane nie są
 * współdzielone między testami dzięki wywołaniu [db.close()] w [teardown].
 *
 * Uruchamianie z Robolectric (brak potrzeby emulatora/urządzenia):
 * ```
 * ./gradlew test --tests "com.flashlearn.data.DeckDaoTest"
 * ```
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeckDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = AppDatabase.buildInMemory(
            ApplicationProvider.getApplicationContext()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildDeck(
        title: String = "Testowa talia",
        isPublic: Boolean = false,
    ) = Deck(title = title, isPublic = isPublic)

    // ── INSERT ────────────────────────────────────────────────────────────────

    @Test
    fun `insert returns valid local id`() = runTest {
        val id = db.deckDao().insert(buildDeck())
        assertTrue("insert powinno zwrócić id > 0", id > 0)
    }

    @Test
    fun `inserted deck can be retrieved by id`() = runTest {
        val id = db.deckDao().insert(buildDeck(title = "Kotlin"))
        val retrieved = db.deckDao().getById(id)

        assertNotNull(retrieved)
        assertEquals("Kotlin", retrieved!!.title)
        assertEquals(id, retrieved.id)
    }

    @Test
    fun `insertAll inserts multiple decks`() = runTest {
        val decks = listOf(
            buildDeck("Matematyka"),
            buildDeck("Fizyka"),
            buildDeck("Chemia"),
        )
        val ids = db.deckDao().insertAll(decks)
        assertEquals(3, ids.size)

        val all = db.deckDao().getAll()
        assertEquals(3, all.size)
    }

    // ── SELECT ────────────────────────────────────────────────────────────────

    @Test
    fun `getById returns null for non-existent id`() = runTest {
        val result = db.deckDao().getById(999L)
        assertNull(result)
    }

    @Test
    fun `observeAll emits updated list after insert`() = runTest {
        db.deckDao().insert(buildDeck("Angielski"))
        db.deckDao().insert(buildDeck("Japoński"))

        val list = db.deckDao().observeAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun `observePublic returns only public decks`() = runTest {
        db.deckDao().insert(buildDeck("Prywatna", isPublic = false))
        db.deckDao().insert(buildDeck("Publiczna", isPublic = true))
        db.deckDao().insert(buildDeck("Też publiczna", isPublic = true))

        val publicDecks = db.deckDao().observePublic().first()
        assertEquals(2, publicDecks.size)
        assertTrue(publicDecks.all { it.isPublic })
    }

    @Test
    fun `getByServerId returns correct deck`() = runTest {
        val localId = db.deckDao().insert(buildDeck("Serwer"))
        db.deckDao().markSynced(localId, serverId = 42L, updatedAt = 1_000_000L)

        val found = db.deckDao().getByServerId(42L)
        assertNotNull(found)
        assertEquals(42L, found!!.serverId)
    }

    @Test
    fun `getPendingSync returns only unsynced decks`() = runTest {
        val id1 = db.deckDao().insert(buildDeck("Nowa"))        // needs_sync = true
        val id2 = db.deckDao().insert(buildDeck("Stara"))
        db.deckDao().markSynced(id2, serverId = 1L, updatedAt = 0L)  // needs_sync = false

        val pending = db.deckDao().getPendingSync()
        assertEquals(1, pending.size)
        assertEquals(id1, pending.first().id)
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    fun `update changes title and returns 1`() = runTest {
        val id = db.deckDao().insert(buildDeck("Stary tytuł"))
        val deck = db.deckDao().getById(id)!!

        val updated = deck.copy(title = "Nowy tytuł")
        val rows = db.deckDao().update(updated)

        assertEquals(1, rows)
        assertEquals("Nowy tytuł", db.deckDao().getById(id)!!.title)
    }

    @Test
    fun `markSynced clears needsSync flag and sets serverId`() = runTest {
        val id = db.deckDao().insert(buildDeck())

        db.deckDao().markSynced(id, serverId = 99L, updatedAt = 2_000_000L)

        val synced = db.deckDao().getById(id)!!
        assertEquals(99L, synced.serverId)
        assertEquals(false, synced.needsSync)
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    fun `delete removes deck from database`() = runTest {
        val id = db.deckDao().insert(buildDeck())
        val deck = db.deckDao().getById(id)!!

        db.deckDao().delete(deck)

        assertNull(db.deckDao().getById(id))
    }

    @Test
    fun `deleteAll clears the table`() = runTest {
        db.deckDao().insertAll(listOf(buildDeck("A"), buildDeck("B")))
        db.deckDao().deleteAll()

        assertTrue(db.deckDao().getAll().isEmpty())
    }
}
