package com.flashlearn.backend.marketplace;

import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Category;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.CategoryRepository;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.FlashcardRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Serwis obsługujący Marketplace — publiczne talie dostępne do klonowania.
 * Endpointy GET /marketplace są publiczne (bez JWT).
 * Endpointy POST /marketplace/publish, /submit, /report i /clone wymagają JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_FLASHCARDS_TO_SUBMIT = 5;

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Zwraca stronicowaną listę publicznych talii.
     * Opcjonalne filtrowanie po slug kategorii.
     * Sortowanie domyślnie po download_count malejąco.
     *
     * @param categorySlug slug kategorii lub null dla wszystkich
     * @param page         numer strony (0-based)
     * @return stronicowana lista talii
     */
    @Transactional(readOnly = true)
    public MarketplacePageResponse getDecks(String categorySlug, int page) {
        PageRequest pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);

        Page<Deck> result = (categorySlug != null && !categorySlug.isBlank())
                ? deckRepository.findPublicByCategorySlug(categorySlug, pageable)
                : deckRepository.findByIsPublicTrueOrderByDownloadCountDesc(pageable);

        return new MarketplacePageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    /**
     * Publikuje talię użytkownika w Marketplace (ustawia isPublic=true).
     * Weryfikuje własność talii.
     *
     * @param request żądanie publikacji z deckId
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public void publish(PublishRequest request) {
        User user = getCurrentUser();
        Deck deck = deckRepository.findById(request.getDeckId())
                .orElseThrow(() -> new DeckNotFoundException(request.getDeckId()));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + request.getDeckId());
        }

        deck.setPublic(true);
        deckRepository.save(deck);
    }

    /**
     * Zgłasza talię do Marketplace z kategorią i opcjonalnym opisem.
     * Auto-akceptacja — talia jest od razu widoczna (isPublic=true).
     * Walidacja: talia musi mieć min. 5 fiszek.
     *
     * @param request żądanie zgłoszenia z deckId, categoryId i opcjonalnym opisem
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     * @throws ResponseStatusException 400 gdy talia ma mniej niż 5 fiszek
     * @throws ResponseStatusException 404 gdy kategoria nie istnieje
     */
    @Transactional
    public void submit(SubmitRequest request) {
        User user = getCurrentUser();

        Deck deck = deckRepository.findById(request.getDeckId())
                .orElseThrow(() -> new DeckNotFoundException(request.getDeckId()));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + request.getDeckId());
        }

        List<Flashcard> flashcards = flashcardRepository.findByDeckId(deck.getId());
        if (flashcards.size() < MIN_FLASHCARDS_TO_SUBMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Deck must have at least " + MIN_FLASHCARDS_TO_SUBMIT + " flashcards to be submitted. " +
                            "Current count: " + flashcards.size());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found: id=" + request.getCategoryId()));

        deck.setCategory(category);
        deck.setPublic(true);
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            deck.setDescription(request.getDescription());
        }

        deckRepository.save(deck);

        log.info("Deck id={} zgłoszony do Marketplace przez użytkownika id={}. Kategoria: {}, fiszek: {}",
                deck.getId(), user.getId(), category.getSlug(), flashcards.size());
    }

    /**
     * Klonuje publiczną talię do biblioteki zalogowanego użytkownika.
     * Tworzy głęboką kopię talii wraz z wszystkimi fiszkami.
     * Inkrementuje download_count oryginalnej talii.
     *
     * @param deckId identyfikator talii do sklonowania
     * @return nowa talia z listą skopiowanych fiszek
     * @throws DeckNotFoundException gdy talia nie istnieje lub nie jest publiczna
     */
    @Transactional
    public CloneResponse clone(Long deckId) {
        User user = getCurrentUser();

        Deck original = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!original.isPublic()) {
            throw new DeckNotFoundException(deckId);
        }

        Deck cloned = Deck.builder()
                .owner(user)
                .title(original.getTitle())
                .description(original.getDescription())
                .category(original.getCategory())
                .isPublic(false)
                .downloadCount(0)
                .build();

        Deck savedDeck = deckRepository.save(cloned);

        List<Flashcard> originalFlashcards = flashcardRepository.findByDeckId(original.getId());

        List<Flashcard> clonedFlashcards = originalFlashcards.stream()
                .map(f -> Flashcard.builder()
                        .deck(savedDeck)
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .build())
                .toList();

        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(clonedFlashcards);

        deckRepository.incrementDownloadCount(original.getId());

        log.info("Deck id={} sklonowany przez uzytkownika id={}. Nowa talia id={}",
                original.getId(), user.getId(), savedDeck.getId());

        List<ClonedFlashcardResponse> flashcardResponses = savedFlashcards.stream()
                .map(f -> new ClonedFlashcardResponse(f.getId(), f.getQuestion(), f.getAnswer()))
                .toList();

        return new CloneResponse(
                savedDeck.getId(),
                savedDeck.getTitle(),
                savedDeck.getDescription(),
                flashcardResponses
        );
    }

    /**
     * Zgłasza talię publiczną jako nieodpowiednią.
     * Weryfikuje czy talia istnieje i jest publiczna.
     * Na tym etapie zgłoszenie jest logowane — bez moderacji.
     *
     * @param request żądanie zgłoszenia z deckId i opcjonalnym powodem
     * @throws DeckNotFoundException gdy talia nie istnieje lub nie jest publiczna
     */
    @Transactional
    public void report(ReportRequest request) {
        Deck deck = deckRepository.findById(request.getDeckId())
                .orElseThrow(() -> new DeckNotFoundException(request.getDeckId()));

        if (!deck.isPublic()) {
            throw new DeckNotFoundException(request.getDeckId());
        }

        String reason = request.getReason() != null ? request.getReason() : "brak";
        log.info("[REPORT] Deck id={} zgłoszony przez użytkownika. Powód: {}", deck.getId(), reason);
    }

    private MarketplaceDeckResponse toResponse(Deck deck) {
        Long categoryId = deck.getCategory() != null ? deck.getCategory().getId() : null;
        String categoryName = deck.getCategory() != null ? deck.getCategory().getName() : null;
        String categorySlug = deck.getCategory() != null ? deck.getCategory().getSlug() : null;
        String categoryIcon = deck.getCategory() != null ? deck.getCategory().getIconName() : null;

        return new MarketplaceDeckResponse(
                deck.getId(),
                deck.getTitle(),
                deck.getDescription(),
                deck.getOwner().getEmail(),
                categoryId,
                categoryName,
                categorySlug,
                categoryIcon,
                deck.getFlashcards() != null ? deck.getFlashcards().size() : 0,
                deck.getDownloadCount(),
                deck.getCreatedAt()
        );
    }

    /**
     * Pobiera zalogowanego użytkownika z SecurityContext.
     *
     * @throws ResponseStatusException 401 gdy brak autentykacji w kontekście
     * @throws ResponseStatusException 401 gdy użytkownik nie istnieje w bazie
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authentication found in security context");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}