package com.flashlearn.data.db
import androidx.sqlite.db.SupportSQLiteDatabase


object SeedData {

    fun insert(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis() / 1000L
        db.beginTransaction()
        try {
            db.execSQL("""
                INSERT OR IGNORE INTO decks 
                (id, title, description, is_public, owner_id, created_at, updated_at, needs_sync, is_readonly, category_slug)
                VALUES (1, 'Angielski B2 — słówka', NULL, 0, NULL, $now, $now, 0, 1, 'jezyki')
            """.trimIndent())
            ENGLISH_B2.forEachIndexed { i, (front, back) ->
                db.execSQL("""
                    INSERT OR IGNORE INTO flashcards 
                    (id, deck_id, question, answer, created_at, updated_at, needs_sync)
                    VALUES (${1000 + i}, 1, '${front.replace("'", "''")}', '${back.replace("'", "''")}', $now, $now, 0)
                """.trimIndent())
            }

            // ── Kotlin/Java ───────────────────────────────────────
            db.execSQL("""
                INSERT OR IGNORE INTO decks 
                (id, title, description, is_public, owner_id, created_at, updated_at, needs_sync, is_readonly, category_slug)
                VALUES (2, 'Kotlin / Java — pojęcia', NULL, 0, NULL, $now, $now, 0, 1, 'programowanie')
            """.trimIndent())
            KOTLIN_JAVA.forEachIndexed { i, (front, back) ->
                db.execSQL("""
                    INSERT OR IGNORE INTO flashcards 
                    (id, deck_id, question, answer, created_at, updated_at, needs_sync)
                    VALUES (${2000 + i}, 2, '${front.replace("'", "''")}', '${back.replace("'", "''")}', $now, $now, 0)
                """.trimIndent())
            }

            // ── Matematyka dyskretna ──────────────────────────────
            db.execSQL("""
                INSERT OR IGNORE INTO decks 
                (id, title, description, is_public, owner_id, created_at, updated_at, needs_sync, is_readonly, category_slug)
                VALUES (3, 'Matematyka dyskretna — definicje', NULL, 0, NULL, $now, $now, 0, 1, 'matematyka')
            """.trimIndent())
            DISCRETE_MATH.forEachIndexed { i, (front, back) ->
                db.execSQL("""
                    INSERT OR IGNORE INTO flashcards 
                    (id, deck_id, question, answer, created_at, updated_at, needs_sync)
                    VALUES (${3000 + i}, 3, '${front.replace("'", "''")}', '${back.replace("'", "''")}', $now, $now, 0)
                """.trimIndent())
            }

            db.setTransactionSuccessful()

        } finally {
            db.endTransaction()
        }
    }
    val ENGLISH_B2 = listOf(
        "Abundant" to "występujący w dużych ilościach",
        "Ambiguous" to "niejednoznaczny, dwuznaczny",
        "Benevolent" to "życzliwy, dobroczynny",
        "Coherent" to "spójny, logiczny",
        "Contemplate" to "rozważać, kontemplować",
        "Detrimental" to "szkodliwy",
        "Eloquent" to "wymowny, elokwentny",
        "Endeavour" to "dążyć, starać się",
        "Feasible" to "wykonalny, możliwy",
        "Fluctuate" to "wahać się, fluktuować",
        "Forthcoming" to "nadchodzący, zbliżający się",
        "Grasp" to "pojąć, uchwycić",
        "Hamper" to "utrudniać, przeszkadzać",
        "Imply" to "sugerować, implikować",
        "Inevitable" to "nieuchronny, nieunikniony",
        "Insight" to "wgląd, spostrzeżenie",
        "Integrity" to "prawość, integralność",
        "Intricate" to "skomplikowany, zawiły",
        "Legitimate" to "legalny, uzasadniony",
        "Mitigate" to "łagodzić, zmniejszać",
        "Notion" to "pojęcie, wyobrażenie",
        "Obsolete" to "przestarzały",
        "Paradox" to "paradoks",
        "Perceive" to "postrzegać, spostrzegać",
        "Profound" to "głęboki, dogłębny",
        "Reluctant" to "niechętny, oporny",
        "Scrutinize" to "przyglądać się uważnie",
        "Subsequent" to "kolejny, następny",
        "Substantial" to "znaczny, istotny",
        "Subtle" to "subtelny, delikatny",
        "Sufficient" to "wystarczający",
        "Surge" to "gwałtowny wzrost",
        "Sustain" to "podtrzymywać, utrzymywać",
        "Tangible" to "namacalny, konkretny",
        "Tendency" to "tendencja, skłonność",
        "Thorough" to "dokładny, gruntowny",
        "Throughout" to "przez cały czas, wszędzie",
        "Trigger" to "wyzwalać, uruchamiać",
        "Undermine" to "podważać, osłabiać",
        "Unprecedented" to "bezprecedensowy",
        "Vague" to "niejasny, mglisty",
        "Viable" to "realny, wykonalny",
        "Vigorous" to "energiczny, intensywny",
        "Volatile" to "niestabilny, zmienny",
        "Witness" to "być świadkiem, obserwować",
        "Yield" to "przynosić, dawać (wynik)",
        "Anticipate" to "przewidywać, spodziewać się",
        "Cease" to "zaprzestać, ustać",
        "Derive" to "wywodzić się, czerpać",
        "Diminish" to "zmniejszać się, maleć"
    )

    val KOTLIN_JAVA = listOf(
        "Coroutine" to "lekki wątek Kotlina — suspendowalna jednostka współbieżności",
        "suspend fun" to "funkcja która może być wstrzymana bez blokowania wątku",
        "Flow" to "zimny strumień asynchronicznych wartości w Kotlinie",
        "StateFlow" to "gorący Flow przechowujący ostatnią wartość — zastępstwo LiveData",
        "ViewModel" to "klasa trzymająca stan UI przeżywająca rekonfigurację",
        "LiveData" to "obserwowalny holder danych świadomy cyklu życia",
        "Extension function" to "funkcja dodana do klasy bez jej dziedziczenia",
        "Data class" to "klasa Kotlina z auto-generowanym equals/hashCode/copy/toString",
        "Sealed class" to "klasa z ograniczonym zbiorem podklas — idealna na Result/State",
        "Companion object" to "singleton powiązany z klasą — odpowiednik static z Javy",
        "Lazy delegation" to "inicjalizacja wartości dopiero przy pierwszym użyciu",
        "Null safety" to "system typów Kotlina eliminujący NullPointerException",
        "Smart cast" to "automatyczne rzutowanie po sprawdzeniu typu przez is",
        "Scope function: let" to "wykonuje blok na obiekcie, zwraca wynik bloku",
        "Scope function: apply" to "wykonuje blok na obiekcie, zwraca obiekt",
        "Scope function: run" to "wykonuje blok w kontekście obiektu, zwraca wynik bloku",
        "Scope function: also" to "wykonuje blok jako efekt uboczny, zwraca obiekt",
        "Higher-order function" to "funkcja przyjmująca lub zwracająca inną funkcję",
        "Lambda" to "anonimowa funkcja przekazywana jako wyrażenie",
        "Interface default method" to "metoda z implementacją w interfejsie (Java 8+)",
        "Generics" to "parametryzacja typów — List<T>, umożliwia reużycie kodu",
        "Variance: covariant" to "out T — można tylko czytać (producent)",
        "Variance: contravariant" to "in T — można tylko zapisywać (konsument)",
        "Reified type" to "typ generyczny dostępny w runtime przez inline fun",
        "Object expression" to "anonimowa implementacja interfejsu lub klasy",
        "Delegation pattern" to "by keyword — delegowanie implementacji do innego obiektu",
        "Infix function" to "funkcja wywoływana bez kropki i nawiasów: a infixFun b",
        "Operator overloading" to "przeciążanie operatorów przez operator fun",
        "Destructuring" to "rozkładanie obiektu na zmienne: val (a, b) = pair",
        "SAM conversion" to "automatyczna konwersja lambdy na interfejs z jedną metodą"
    )

    val DISCRETE_MATH = listOf(
        "Zbiór" to "kolekcja różnych elementów, np. A = {1, 2, 3}",
        "Podzbiór" to "A ⊆ B gdy każdy element A należy do B",
        "Zbiór potęgowy" to "P(A) — zbiór wszystkich podzbiorów A, |P(A)| = 2^n",
        "Suma zbiorów" to "A ∪ B — elementy należące do A lub B",
        "Iloczyn zbiorów" to "A ∩ B — elementy należące do A i do B",
        "Różnica zbiorów" to "A \\ B — elementy A które nie należą do B",
        "Dopełnienie" to "A' — elementy nie należące do A (względem uniwersum)",
        "Relacja" to "podzbiór iloczynu kartezjańskiego A × B",
        "Relacja równoważności" to "zwrotna, symetryczna i przechodnia",
        "Klasa abstrakcji" to "zbiór elementów równoważnych danemu elementowi",
        "Funkcja" to "relacja gdzie każdy element dziedziny ma dokładnie jeden obraz",
        "Funkcja iniekcyjna" to "różnowartościowa — różne argumenty dają różne obrazy",
        "Funkcja surjekcyjna" to "na — każdy element przeciwdziedziny jest obrazem",
        "Funkcja bijekcyjna" to "iniekcja + suriekcja — wzajemnie jednoznaczna",
        "Graf" to "G = (V, E) — zbiór wierzchołków i krawędzi",
        "Graf skierowany" to "krawędzie mają kierunek — digraf",
        "Stopień wierzchołka" to "liczba krawędzi incydentnych z wierzchołkiem",
        "Ścieżka" to "ciąg wierzchołków połączonych krawędziami bez powtórzeń",
        "Cykl" to "ścieżka zaczynająca i kończąca się w tym samym wierzchołku",
        "Drzewo" to "spójny graf acykliczny, |E| = |V| - 1",
        "Drzewo rozpinające" to "drzewo zawierające wszystkie wierzchołki grafu",
        "Indukcja matematyczna" to "baza + krok indukcyjny → twierdzenie dla wszystkich n",
        "Zasada szufladkowa" to "n+1 obiektów w n szufladach → jakaś ma ≥ 2",
        "Symbol Newtona" to "C(n,k) = n! / (k!(n-k)!) — liczba k-elementowych podzbiorów",
        "Algorytm" to "skończony, deterministyczny ciąg kroków rozwiązujący problem"
    )

}