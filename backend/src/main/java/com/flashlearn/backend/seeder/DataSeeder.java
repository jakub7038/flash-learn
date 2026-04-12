package com.flashlearn.backend.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${server.port:8080}")
    private int port;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== DataSeeder starting ===");

        seedUser("test@test.com",         "test1234", karolDecks());
        seedUser("KarolWalicki@ur.pl",   "haslo123", karolDecks());
        seedUser("AnnaKowalska@ur.pl",   "haslo123", annaDecks());
        seedUser("PiotrNowak@ur.pl",     "haslo123", piotrDecks());
        seedUser("MariaWisniewski@ur.pl","haslo123", mariaDecks());

        log.info("=== DataSeeder finished ===");
    }

    // ── seed one user ─────────────────────────────────────────────────────────

    private void seedUser(String email, String password, List<SeedDeck> decks) {
        try {
            register(email, password);
        } catch (HttpClientErrorException.Conflict e) {
            log.info("User {} already exists – skipping", email);
            return;
        } catch (Exception e) {
            log.warn("Could not register {}: {}", email, e.getMessage());
            return;
        }

        String token;
        try {
            token = login(email, password);
        } catch (Exception e) {
            log.warn("Could not login {}: {}", email, e.getMessage());
            return;
        }

        for (SeedDeck deck : decks) {
            try {
                Long deckId = createDeck(token, deck.title(), deck.description(), deck.isPublic());
                for (SeedCard card : deck.cards()) {
                    createFlashcard(token, deckId, card.question(), card.answer());
                }
                log.info("  created deck '{}' with {} cards", deck.title(), deck.cards().size());
            } catch (Exception e) {
                log.warn("  failed deck '{}': {}", deck.title(), e.getMessage());
            }
        }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private void register(String email, String password) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange(
                baseUrl() + "/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), h),
                Void.class);
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), h),
                String.class);
        Map<String, Object> body = mapper.readValue(resp.getBody(), Map.class);
        return (String) body.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Long createDeck(String token, String title, String description, boolean isPublic) throws Exception {
        HttpHeaders h = authHeaders(token);
        ResponseEntity<String> resp = rest.exchange(
                baseUrl() + "/decks",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("title", title, "description", description, "isPublic", isPublic), h),
                String.class);
        Map<String, Object> body = mapper.readValue(resp.getBody(), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    private void createFlashcard(String token, Long deckId, String question, String answer) {
        HttpHeaders h = authHeaders(token);
        rest.exchange(
                baseUrl() + "/decks/" + deckId + "/flashcards",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("question", question, "answer", answer), h),
                Void.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    // ── seed data definitions ─────────────────────────────────────────────────

    private List<SeedDeck> karolDecks() {
        return List.of(
            new SeedDeck("Angielski – poziom B2", "Słownictwo i gramatyka na poziom B2", false, List.of(
                new SeedCard("What does 'ubiquitous' mean?", "Present or found everywhere – wszechobecny"),
                new SeedCard("What does 'eloquent' mean?", "Fluent and persuasive in speaking – elokwentny"),
                new SeedCard("What does 'ambiguous' mean?", "Open to more than one interpretation – niejednoznaczny"),
                new SeedCard("What does 'deteriorate' mean?", "Become progressively worse – pogarszać się"),
                new SeedCard("What does 'meticulous' mean?", "Showing great attention to detail – skrupulatny"),
                new SeedCard("What does 'volatile' mean?", "Liable to change rapidly and unpredictably – niestabilny, lotny")
            )),
            new SeedDeck("Matematyka dyskretna", "Podstawy matematyki dyskretnej", false, List.of(
                new SeedCard("Co to jest graf skierowany?", "Graf, w którym krawędzie mają określony kierunek (łuk)."),
                new SeedCard("Co to jest drzewo rozpinające?", "Podgraf spójny i acykliczny zawierający wszystkie wierzchołki grafu."),
                new SeedCard("Co to jest liczba pierwsza?", "Liczba naturalna większa od 1, która ma dokładnie dwa dzielniki: 1 i siebie samą."),
                new SeedCard("Co mówi małe twierdzenie Fermata?", "Jeśli p jest liczbą pierwszą i NWD(a,p)=1, to a^(p-1) ≡ 1 (mod p)."),
                new SeedCard("Co to jest permutacja?", "Każde wzajemnie jednoznaczne odwzorowanie zbioru n-elementowego na siebie.")
            )),
            new SeedDeck("PHP: Hypertext Preprocessor", "Ciekawostki o PHP", true, List.of(
                new SeedCard("Co znaczy skrót PHP?", "PHP: Hypertext Preprocessor – rekurencyjny akronim, bo PHP jest w środku PHP."),
                new SeedCard("Kiedy powstał PHP?", "W 1994 roku, stworzony przez Rasmusa Lerdorfa jako zbiór skryptów CGI w Perlu."),
                new SeedCard("Dlaczego PHP jest nazywany 'fraktalem złego designu'?", "Bo każda jego część z osobna i jako całość jednocześnie łamie zasady dobrego projektowania."),
                new SeedCard("Co zwraca strpos('abc', 'a')?", "0 – ale uwaga: 0 == false w PHP, więc zawsze sprawdzaj przez === false!"),
                new SeedCard("Czy HTML to język programowania?", "Nie. HTML to język znaczników (markup language) – opisuje strukturę treści, nie zawiera logiki, pętli ani warunków.")
            )),
            new SeedDeck("Systemy Operacyjne 2", "Pytania egzaminacyjne z SO2", false, so2Cards()),
            new SeedDeck("Scrum i Kanban", "Metodyki zwinne – Scrum i Kanban", false, scrumCards()),
            new SeedDeck("Git i GitHub", "Podstawy kontroli wersji", false, gitCards())
        );
    }

    private List<SeedDeck> annaDecks() {
        return List.of(
            new SeedDeck("Sieci komputerowe", "Podstawy sieci – model OSI, protokoły", false, List.of(
                new SeedCard("Ile warstw ma model OSI?", "7 warstw: fizyczna, łącza danych, sieciowa, transportowa, sesji, prezentacji, aplikacji."),
                new SeedCard("Do czego służy protokół ARP?", "Address Resolution Protocol – tłumaczy adres IP na adres MAC w sieci lokalnej."),
                new SeedCard("Czym różni się TCP od UDP?", "TCP jest połączeniowy i gwarantuje dostarczenie; UDP jest bezpołączeniowy i szybszy."),
                new SeedCard("Co to jest maska podsieci?", "Liczba 32-bitowa określająca, która część adresu IP to sieć, a która to host."),
                new SeedCard("Co to jest DNS?", "Domain Name System – tłumaczy nazwy domenowe (np. google.com) na adresy IP.")
            )),
            new SeedDeck("Bazy danych", "SQL i teoria relacyjna", false, List.of(
                new SeedCard("Co to jest klucz główny?", "Kolumna lub zestaw kolumn jednoznacznie identyfikujących każdy wiersz w tabeli."),
                new SeedCard("Co to jest normalizacja bazy danych?", "Proces organizowania danych w celu redukcji redundancji i poprawy integralności danych."),
                new SeedCard("Co robi polecenie JOIN?", "Łączy wiersze z dwóch lub więcej tabel na podstawie powiązanej kolumny."),
                new SeedCard("Czym różni się INNER JOIN od LEFT JOIN?", "INNER JOIN zwraca tylko pasujące wiersze; LEFT JOIN zwraca wszystkie wiersze z lewej tabeli."),
                new SeedCard("Co to jest indeks w bazie danych?", "Struktura danych przyspieszająca wyszukiwanie wierszy w tabeli kosztem dodatkowego miejsca.")
            ))
        );
    }

    private List<SeedDeck> piotrDecks() {
        return List.of(
            new SeedDeck("Algorytmy i struktury danych", "Sortowanie, wyszukiwanie, złożoność", false, List.of(
                new SeedCard("Jaka jest złożoność czasowa QuickSort (średni przypadek)?", "O(n log n)"),
                new SeedCard("Jaka jest złożoność MergeSort?", "O(n log n) – zarówno w najlepszym, średnim, jak i najgorszym przypadku."),
                new SeedCard("Co to jest stos (stack)?", "Struktura danych LIFO – Last In First Out. Operacje: push i pop."),
                new SeedCard("Co to jest kolejka (queue)?", "Struktura danych FIFO – First In First Out. Operacje: enqueue i dequeue."),
                new SeedCard("Co to jest wyszukiwanie binarne?", "Algorytm wyszukujący w posortowanej tablicy przez dzielenie zakresu na pół – O(log n).")
            )),
            new SeedDeck("Wzorce projektowe", "GoF Design Patterns", false, List.of(
                new SeedCard("Co to jest wzorzec Singleton?", "Zapewnia, że klasa ma tylko jedną instancję i udostępnia do niej globalny punkt dostępu."),
                new SeedCard("Co to jest wzorzec Observer?", "Definiuje zależność jeden-do-wielu: gdy obiekt zmienia stan, wszyscy zależni są powiadamiani."),
                new SeedCard("Co to jest wzorzec Factory Method?", "Definiuje interfejs do tworzenia obiektów, ale pozwala podklasom decydować, jaką klasę instancjonować."),
                new SeedCard("Co to jest wzorzec Strategy?", "Definiuje rodzinę algorytmów, kapsułkuje każdy z nich i umożliwia ich wymienność w runtime."),
                new SeedCard("Co to jest wzorzec Decorator?", "Dynamicznie dodaje obowiązki do obiektu bez zmiany jego klasy – alternatywa dla dziedziczenia.")
            ))
        );
    }

    private List<SeedDeck> mariaDecks() {
        return List.of(
            new SeedDeck("Java – podstawy", "Składnia i koncepcje Java", false, List.of(
                new SeedCard("Co to jest polimorfizm?", "Zdolność obiektu do przyjmowania wielu form – metoda może być nadpisana w podklasie."),
                new SeedCard("Czym różni się interface od abstract class?", "Interface definiuje kontrakt (tylko metody abstrakcyjne do Java 8); abstract class może mieć implementacje."),
                new SeedCard("Co to jest garbage collector?", "Automatyczny mechanizm zarządzania pamięcią – usuwa obiekty, do których nie ma referencji."),
                new SeedCard("Co to jest wyjątek checked vs unchecked?", "Checked: musi być obsłużony lub zadeklarowany (IOException). Unchecked: dziedziczy po RuntimeException."),
                new SeedCard("Co robi słowo kluczowe 'final'?", "Dla zmiennej: stała. Dla metody: nie można nadpisać. Dla klasy: nie można dziedziczyć.")
            ))
        );
    }

    // ── SO2 flashcards ────────────────────────────────────────────────────────

    private List<SeedCard> so2Cards() {
        return List.of(
            new SeedCard("Co to jest zakleszczenie (deadlock)?",
                "Stan, w którym dwa lub więcej procesów czeka na zasoby zajęte przez siebie nawzajem, uniemożliwiając dalsze działanie."),
            new SeedCard("Jakie są warunki konieczne zakleszczenia (warunki Coffmana)?",
                "1) Wzajemne wykluczanie, 2) Czekanie z trzymaniem, 3) Brak wywłaszczania, 4) Czekanie cykliczne."),
            new SeedCard("Co to jest semafor?",
                "Zmienna całkowita używana do synchronizacji procesów. Operacje: wait (P) – dekrementuje; signal (V) – inkrementuje."),
            new SeedCard("Czym różni się semafor binarny od muteksu?",
                "Muteks ma właściciela – tylko wątek, który go zablokował, może odblokować. Semafor binarny może być zwolniony przez inny wątek."),
            new SeedCard("Co to jest problem producent-konsument?",
                "Problem synchronizacji: producent dodaje elementy do bufora, konsument pobiera. Bufor ma ograniczony rozmiar – wymagana synchronizacja."),
            new SeedCard("Co to jest wywłaszczanie (preemption) w kontekście schedulera?",
                "Mechanizm pozwalający systemowi operacyjnemu przerwać działający proces i przekazać CPU innemu procesowi."),
            new SeedCard("Czym różni się proces od wątku?",
                "Proces ma własną przestrzeń adresową. Wątki współdzielą przestrzeń adresową procesu. Tworzenie wątku jest tańsze niż procesu."),
            new SeedCard("Co to jest algorytm planowania Round Robin?",
                "Każdy proces dostaje kwant czasu CPU po kolei. Po upłynięciu kwantu proces trafia na koniec kolejki."),
            new SeedCard("Co to jest pamięć wirtualna?",
                "Abstrakcja pozwalająca procesowi mieć więcej pamięci niż fizycznie dostępna RAM. Realizowana przez stronicowanie i/lub segmentację."),
            new SeedCard("Co to jest stronicowanie (paging)?",
                "Podział przestrzeni adresowej na strony (pages) i pamięci fizycznej na ramki (frames). Mapowanie realizuje tablica stron (page table)."),
            new SeedCard("Co to jest błąd strony (page fault)?",
                "Wyjątek gdy proces odwołuje się do strony nieobecnej w RAM. System ładuje stronę z dysku (swap) do pamięci."),
            new SeedCard("Co to jest thrashing?",
                "Stan, w którym system spędza więcej czasu na obsłudze błędów stron (swap) niż na właściwej pracy procesów."),
            new SeedCard("Czym różni się algorytm LRU od FIFO w wymianie stron?",
                "LRU (Least Recently Used) usuwa stronę najdawniej używaną. FIFO usuwa najstarszą załadowaną stronę (nie zawsze optymalnie)."),
            new SeedCard("Co to jest sekcja krytyczna?",
                "Fragment kodu, w którym proces uzyskuje dostęp do zasobu współdzielonego. Tylko jeden proces może być w sekcji krytycznej jednocześnie."),
            new SeedCard("Jakie warunki musi spełniać rozwiązanie sekcji krytycznej?",
                "1) Wzajemne wykluczanie, 2) Postęp (jeśli nikt nie jest w sekcji, decyzja zapada w skończonym czasie), 3) Ograniczone czekanie."),
            new SeedCard("Co to jest monitor (w sensie synchronizacji)?",
                "Wysoko poziomowy mechanizm synchronizacji – klasa/moduł z danymi i metodami; tylko jeden wątek może wykonywać metodę monitora w danej chwili."),
            new SeedCard("Co to jest problem pięciu filozofów?",
                "Problem synchronizacji ilustrujący zakleszczenie i zagłodzenie. Pięciu filozofów siedzi przy okrągłym stole; każdy potrzebuje dwóch widelców."),
            new SeedCard("Co to jest zagłodzenie (starvation)?",
                "Stan, w którym proces nigdy nie dostaje zasobu (np. CPU), bo inne procesy są stale preferowane przez scheduler."),
            new SeedCard("Co to jest IPC (Inter-Process Communication)?",
                "Mechanizmy komunikacji między procesami: potoki (pipes), kolejki komunikatów, pamięć współdzielona, gniazda, sygnały."),
            new SeedCard("Co to jest copy-on-write (COW)?",
                "Optymalizacja fork(): dziecko współdzieli strony z rodzicem do momentu zapisu. Przy zapisie tworzona jest kopia tylko zmodyfikowanej strony.")
        );
    }

    // ── Scrum & Kanban flashcards ─────────────────────────────────────────────

    private List<SeedCard> scrumCards() {
        return List.of(
            new SeedCard("Jakie są trzy role w Scrumie?",
                "Product Owner, Scrum Master, Developers (Zespół Deweloperski)."),
            new SeedCard("Co to jest Sprint?",
                "Ograniczony czasowo cykl pracy (zwykle 1–4 tygodnie), po którym powstaje potencjalnie wdrażalny przyrost produktu."),
            new SeedCard("Co to jest Product Backlog?",
                "Uporządkowana lista wszystkich funkcji, wymagań i poprawek do zrealizowania w produkcie. Zarządza nim Product Owner."),
            new SeedCard("Co to jest Sprint Review?",
                "Ceremonia na końcu Sprintu – prezentacja przyrostu interesariuszom i zebranie feedbacku. Aktualizacja Product Backlogu."),
            new SeedCard("Co to jest Sprint Retrospective?",
                "Spotkanie zespołu po Sprint Review – analiza procesu, identyfikacja ulepszeń na kolejny Sprint."),
            new SeedCard("Czym różni się Scrum od Kanbana?",
                "Scrum: iteracje (sprinty), role, ograniczone WIP przez planowanie sprintu. Kanban: przepływ ciągły, limit WIP na kolumny, brak ról."),
            new SeedCard("Co to jest WIP limit w Kanbanie?",
                "Work In Progress limit – maksymalna liczba zadań w danej kolumnie. Redukuje wielozadaniowość i uwidacznia wąskie gardła."),
            new SeedCard("Co to jest Definition of Done (DoD)?",
                "Wspólna definicja kryteriów, które musi spełnić element backlogu, by uznać go za ukończony."),
            new SeedCard("Co to jest Daily Scrum (Daily Standup)?",
                "15-minutowe codzienne spotkanie zespołu deweloperskiego: co zrobiłem wczoraj, co zrobię dziś, czy są przeszkody."),
            new SeedCard("Co to jest Velocity w Scrumie?",
                "Miara ilości pracy (story points lub zadań) ukończonej przez zespół w jednym sprincie. Używana do prognozowania.")
        );
    }

    // ── Git & GitHub flashcards ───────────────────────────────────────────────

    private List<SeedCard> gitCards() {
        return List.of(
            new SeedCard("Co robi polecenie git rebase?",
                "Przenosi lub łączy serię commitów na nową bazę. Przepisuje historię – używaj ostrożnie na gałęziach publicznych."),
            new SeedCard("Czym różni się git merge od git rebase?",
                "Merge tworzy commit scalający, zachowując historię. Rebase przepisuje historię, tworząc liniową sekwencję commitów."),
            new SeedCard("Co to jest git cherry-pick?",
                "Polecenie kopiujące konkretny commit z jednej gałęzi na inną bez scalania całej gałęzi."),
            new SeedCard("Co to jest git stash?",
                "Tymczasowe odkładanie niezacommitowanych zmian na stos, by móc przełączyć gałąź. Przywrócenie: git stash pop."),
            new SeedCard("Co to jest Pull Request (PR)?",
                "Propozycja scalenia gałęzi w GitHub. Umożliwia code review, dyskusję i CI przed scaleniem do głównej gałęzi."),
            new SeedCard("Co robi git reset --hard HEAD?",
                "Cofa roboczy katalog i indeks (staging) do ostatniego commita. Niezapisane zmiany są bezpowrotnie utracone."),
            new SeedCard("Co to jest .gitignore?",
                "Plik konfiguracyjny określający, które pliki i katalogi Git ma ignorować (nie śledzić). Każda linia to wzorzec."),
            new SeedCard("Co to jest git bisect?",
                "Narzędzie do binarnego wyszukiwania commita, który wprowadził błąd. Oznaczasz commit jako good/bad, Git przeszukuje historię."),
            new SeedCard("Czym różni się fork od clone?",
                "Fork tworzy kopię repozytorium na GitHubie (na Twoim koncie). Clone pobiera repozytorium lokalnie na dysk."),
            new SeedCard("Co to jest git tag?",
                "Trwały wskaźnik na konkretny commit, używany zazwyczaj do oznaczania wersji (np. v1.0.0). Typy: lightweight i annotated.")
        );
    }

    // ── record helpers ────────────────────────────────────────────────────────

    private record SeedDeck(String title, String description, boolean isPublic, List<SeedCard> cards) {}
    private record SeedCard(String question, String answer) {}
}
