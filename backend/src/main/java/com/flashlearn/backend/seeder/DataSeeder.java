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

/**
 * Seeds the database with a single test user (test@test.com) and
 * 4 decks containing 2 flashcards each (8 flashcards total).
 * Runs only when the "dev" Spring profile is active.
 */
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
        seedUser("tester@test.com", "test1234", testDecks());
        log.info("=== DataSeeder finished ===");
    }

    // ── seed one user ──────────────────────────────────────────────────────────

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

    // ── HTTP helpers ───────────────────────────────────────────────────────────

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

    // ── seed data: 4 decks × 6 flashcards = 24 flashcards total ──────────────

    private List<SeedDeck> testDecks() {
        return List.of(
            new SeedDeck("Angielski – poziom B2", "Słownictwo na poziomie B2", false, List.of(
                new SeedCard("What does 'ubiquitous' mean?",
                             "Present or found everywhere – wszechobecny"),
                new SeedCard("What does 'eloquent' mean?",
                             "Fluent and persuasive in speaking – elokwentny"),
                new SeedCard("What does 'meticulous' mean?",
                             "Showing great attention to detail; very careful and precise – drobiazgowy"),
                new SeedCard("What does 'resilient' mean?",
                             "Able to withstand or recover quickly from difficult conditions – odporny, szybko wracający do siebie"),
                new SeedCard("What does 'ambiguous' mean?",
                             "Open to more than one interpretation; having a double meaning – dwuznaczny"),
                new SeedCard("What does 'diligent' mean?",
                             "Having or showing care and conscientiousness in one's work or duties – pilny, staranny")
            )),
            new SeedDeck("Matematyka dyskretna", "Podstawy matematyki dyskretnej", false, List.of(
                new SeedCard("Co to jest graf skierowany?",
                             "Graf, w którym krawędzie mają określony kierunek (łuk)."),
                new SeedCard("Co to jest drzewo rozpinające?",
                             "Podgraf spójny i acykliczny zawierający wszystkie wierzchołki grafu."),
                new SeedCard("Co to jest graf dwudzielny?",
                             "Graf, którego wierzchołki można podzielić na dwa rozłączne zbiory tak, aby krawędzie łączyły jedynie wierzchołki z różnych zbiorów."),
                new SeedCard("Czym jest cykl Eulera?",
                             "Cykl w grafie, który przechodzi przez każdą krawędź dokładnie jeden raz."),
                new SeedCard("Co to jest relacja równoważności?",
                             "Relacja, która jest jednocześnie zwrotna, symetryczna i przechodnia."),
                new SeedCard("Ile wynosi suma stopni wierzchołków w dowolnym grafie?",
                             "Suma stopni wszystkich wierzchołków jest równa podwojonej liczbie krawędzi w tym grafie (Lemat o uściskach dłoni).")
            )),
            new SeedDeck("Algorytmy i struktury danych", "Sortowanie, złożoność, struktury", false, List.of(
                new SeedCard("Jaka jest złożoność QuickSort (średni przypadek)?",
                             "O(n log n)"),
                new SeedCard("Co to jest stos (stack)?",
                             "Struktura danych LIFO – Last In First Out. Operacje: push i pop."),
                new SeedCard("Co to jest kolejka (queue)?",
                             "Struktura danych FIFO – First In First Out. Elementy dodaje się na koniec, a pobiera z początku."),
                new SeedCard("Na czym polega wyszukiwanie binarne (Binary Search)?",
                             "Wyszukiwanie elementu w posortowanej tablicy poprzez wielokrotny podział przedziału poszukiwań na połowy. Złożoność: O(log n)."),
                new SeedCard("Co to jest tablica mieszająca (Hash Table)?",
                             "Struktura danych mapująca klucze na wartości za pomocą funkcji haszującej w celu szybkiego wyszukiwania, dodawania i usuwania (O(1) w średnim przypadku)."),
                new SeedCard("Jaka jest optymalna złożoność algorytmu Merge Sort?",
                             "O(n log n) we wszystkich przypadkach: optymistycznym, średnim i pesymistycznym.")
            )),
            new SeedDeck("Systemy Operacyjne", "Podstawowe pojęcia z SO", false, List.of(
                new SeedCard("Co to jest zakleszczenie (deadlock)?",
                             "Stan, w którym procesy czekają na zasoby zajęte przez siebie nawzajem, co uniemożliwia ich dalsze wykonanie."),
                new SeedCard("Czym różni się proces od wątku?",
                             "Proces ma własną przestrzeń adresową i zasoby. Wątki działają w ramach jednego procesu i współdzielą jego przestrzeń."),
                new SeedCard("Co to jest stronicowanie pamięci (paging)?",
                             "Mechanizm zarządzania pamięcią dzielący pamięć fizyczną na ramki, a logiczną na strony o stałym rozmiarze, by zminimalizować fragmentację zewnętrzną."),
                new SeedCard("Do czego służy semafor w SO?",
                             "Do synchronizacji procesów/wątków. Jest to zmienna ułatwiająca wzajemne wykluczanie dostępu do zasobów krytycznych."),
                new SeedCard("Co to jest system plików (file system)?",
                             "Sposób oraz struktura logiczna używana przez system operacyjny do kontrolowania, jak dane są zapisywane i odczytywane na nośniku."),
                new SeedCard("Na czym polega wywłaszczanie (preemption)?",
                             "Na czasowym przerwaniu przez SO wykonywania procesu (bez jego zgody), by przekazać procesor innemu procesowi (np. z powodu przerwania zegarowego).")
            ))
        );
    }

    // ── record helpers ─────────────────────────────────────────────────────────

    private record SeedDeck(String title, String description, boolean isPublic, List<SeedCard> cards) {}
    private record SeedCard(String question, String answer) {}
}
