package com.flashlearn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.entity.Deck
import com.flashlearn.data.entity.Flashcard

/**
 * Główna baza danych Room aplikacji FlashLearn.
 *
 * Wersja 1 obejmuje:
 * - tabelę [Deck]  – talie fiszek użytkownika
 * - tabelę [Flashcard] – fiszki z parametrami SM-2
 *
 * ### Migracje
 * Przy każdej zmianie schematu (nowe kolumny, tabele) należy:
 * 1. Zwiększyć [version] o 1.
 * 2. Dodać obiekt `Migration(stara, nowa)` i przekazać go do buildera.
 * 3. Nigdy nie używać `fallbackToDestructiveMigration()` na produkcji
 *    – użytkownik straciłby lokalnie przechowywane postępy nauki.
 *
 * ### Singleton
 * Instancja jest tworzona raz i współdzielona przez cały proces aplikacji.
 * Zalecane wstrzykiwanie przez Hilt/DI, a nie bezpośrednie wywołanie
 * [getInstance].
 */
@Database(
    entities = [Deck::class, Flashcard::class],
    version = 1,
    exportSchema = true,          // generuje JSON schema do kontroli wersji
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao

    companion object {

        private const val DB_NAME = "flashlearn.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Zwraca singleton bazy danych.
         *
         * Używaj tej metody tylko tam, gdzie DI (Hilt) nie jest dostępny
         * (np. `ContentProvider`, `Application.onCreate`).
         * W pozostałych miejscach preferuj wstrzykiwanie przez konstruktor.
         */
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            )
                // Tutaj dodawać kolejne migracje:
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /**
         * Tworzy bazę danych **in-memory** do użycia w testach jednostkowych
         * i instrumentacyjnych.
         *
         * Baza istnieje wyłącznie w RAM i jest automatycznie niszczona po
         * zamknięciu połączenia – żadne dane nie są zapisywane na dysku.
         *
         * Przykład użycia w teście:
         * ```kotlin
         * private lateinit var db: AppDatabase
         *
         * @Before
         * fun setup() {
         *     db = AppDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
         * }
         *
         * @After
         * fun teardown() {
         *     db.close()
         * }
         * ```
         */
        fun buildInMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()   // dopuszczalne wyłącznie w testach
                .build()
    }
}
