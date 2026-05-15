package com.flashlearn.backend.sync;

import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.CategoryRepository;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.FlashcardRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serwis obslugujacy synchronizacje danych z urzadzenia mobilnego do serwera.
 * Strategia rozwiazywania konfliktow: last-write-wins - wygrywa strona z nowszym updatedAt.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Przetwarza zmiany przyslane z urzadzenia mobilnego.
     * Dla kazdej talii i fiszki stosuje strategie last-write-wins przy konflikcie.
     *
     * @param request lista zmian z urzadzenia wraz z timestamp klienta
     * @return podsumowanie przetworzonych zmian i lista konfliktow
     * @throws ResourceAccessDeniedException gdy uzytkownik probuje modyfikowac cudze zasoby
     */
    @Transactional
    public SyncPushResponse push(SyncPushRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> conflicts = new ArrayList<>();
        Map<Long, Long> deckIdMapping = new HashMap<>();
        Map<Long, Long> flashcardIdMapping = new HashMap<>();
        int decksProcessed = 0;
        int flashcardsProcessed = 0;

        // Przetwarzanie talii
        if (request.getDecks() != null) {
            for (SyncDeckDTO dto : request.getDecks()) {
                String conflict = processDeck(dto, user, request.getClientTimestamp(), deckIdMapping);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                decksProcessed++;
            }
        }

        // Przetwarzanie fiszek
        if (request.getFlashcards() != null) {
            for (SyncFlashcardDTO dto : request.getFlashcards()) {
                String conflict = processFlashcard(dto, user, request.getClientTimestamp(), flashcardIdMapping);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                flashcardsProcessed++;
            }
        }

        return new SyncPushResponse(decksProcessed, flashcardsProcessed, conflicts, deckIdMapping, flashcardIdMapping, LocalDateTime.now());
    }

    /**
     * Pobiera dane uzytkownika zmienione po wskazanym timestamp.
     * Wyniki sa stronicowane.
     *
     * @param since    timestamp od ktorego pobieramy zmiany
     * @param page     numer strony (0-based)
     * @param pageSize liczba wynikow na strone
     * @return dane zmienione po since wraz z metadanymi paginacji
     */
    @Transactional(readOnly = true)
    public SyncPullResponse pull(LocalDateTime since, int page, int pageSize) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, pageSize);

        Page<Deck> deckPage = deckRepository.findByOwnerIdAndUpdatedAtAfter(user.getId(), since, pageable);
        Page<Flashcard> flashcardPage = flashcardRepository.findByDeckOwnerIdAndUpdatedAtAfter(user.getId(), since, pageable);

        long totalDecks = deckRepository.countByOwnerIdAndUpdatedAtAfter(user.getId(), since);
        long totalFlashcards = flashcardRepository.countByDeckOwnerIdAndUpdatedAtAfter(user.getId(), since);

        List<SyncDeckDTO> deckDTOs = deckPage.getContent().stream().map(deck -> {
            SyncDeckDTO dto = new SyncDeckDTO();
            dto.setId(deck.getId());
            dto.setTitle(deck.getTitle());
            dto.setDescription(deck.getDescription());
            dto.setPublic(deck.isPublic());
            dto.setCategorySlug(deck.getCategory() != null ? deck.getCategory().getSlug() : null);
            dto.setUpdatedAt(deck.getUpdatedAt());
            dto.setFlashcards(List.of());
            return dto;
        }).collect(Collectors.toList());

        List<SyncFlashcardDTO> flashcardDTOs = flashcardPage.getContent().stream().map(f -> {
            SyncFlashcardDTO dto = new SyncFlashcardDTO();
            dto.setId(f.getId());
            dto.setDeckId(f.getDeck().getId());
            dto.setQuestion(f.getQuestion());
            dto.setAnswer(f.getAnswer());
            dto.setUpdatedAt(f.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        boolean hasMore = deckPage.hasNext() || flashcardPage.hasNext();

        return new SyncPullResponse(deckDTOs, flashcardDTOs, LocalDateTime.now(),
                page, pageSize, totalDecks, totalFlashcards, hasMore);
    }

    /**
     * Przetwarza pojedyncza talie - tworzy nowa lub aktualizuje istniejaca.
     * Przy konflikcie stosowana jest strategia last-write-wins: wygrywa strona z nowszym updatedAt.
     *
     * @param dto             dane talii z urzadzenia
     * @param owner           zalogowany uzytkownik
     * @param clientTimestamp timestamp ostatniej synchronizacji klienta
     * @param deckIdMapping   mapa lokalnych id na id serwera
     * @return opis konfliktu lub null jesli brak konfliktu
     * @throws ResourceAccessDeniedException gdy talia nalezy do innego uzytkownika
     */
    private String processDeck(SyncDeckDTO dto, User owner, LocalDateTime clientTimestamp, Map<Long, Long> deckIdMapping) {
        if (dto.getId() == null) {
            // Nowa talia — zapisz
            Deck deck = Deck.builder()
                    .owner(owner)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .isPublic(dto.isPublic())
                    .category(dto.getCategorySlug() != null
                            ? categoryRepository.findBySlug(dto.getCategorySlug()).orElse(null)
                            : null)
                    .build();
            deck = deckRepository.save(deck);
            if (dto.getLocalId() != null) {
                deckIdMapping.put(dto.getLocalId(), deck.getId());
            }
            return null;
        }

        // Istniejąca talia — sprawdź własność i konflikt
        return deckRepository.findById(dto.getId()).map(existing -> {
            if (!existing.getOwner().getId().equals(owner.getId())) {
                throw new ResourceAccessDeniedException(
                        "Access denied: deck id=" + dto.getId() + " belongs to another user");
            }

            if (existing.getUpdatedAt() != null &&
                    existing.getUpdatedAt().isAfter(clientTimestamp)) {
                // Konflikt — serwer był edytowany po ostatnim sync klienta — last-write-wins
                if (dto.getUpdatedAt() != null && dto.getUpdatedAt().isAfter(existing.getUpdatedAt())) {
                    existing.setTitle(dto.getTitle());
                    existing.setDescription(dto.getDescription());
                    existing.setPublic(dto.isPublic());
                    existing.setCategory(dto.getCategorySlug() != null
                            ? categoryRepository.findBySlug(dto.getCategorySlug()).orElse(null)
                            : null);
                    deckRepository.save(existing);
                    return "Deck conflict (client-wins): id=" + dto.getId();
                }
                return "Deck conflict (server-wins): id=" + dto.getId();
            }

            // Brak konfliktu — aktualizuj
            existing.setTitle(dto.getTitle());
            existing.setDescription(dto.getDescription());
            existing.setPublic(dto.isPublic());
            existing.setCategory(dto.getCategorySlug() != null
                    ? categoryRepository.findBySlug(dto.getCategorySlug()).orElse(null)
                    : null);
            deckRepository.save(existing);
            return null;
        }).orElse(null);
    }

    /**
     * Przetwarza pojedyncza fiszke - tworzy nowa lub aktualizuje istniejaca.
     * Przy konflikcie stosowana jest strategia last-write-wins: wygrywa strona z nowszym updatedAt.
     *
     * @param dto                  dane fiszki z urzadzenia
     * @param owner                zalogowany uzytkownik
     * @param clientTimestamp      timestamp ostatniej synchronizacji klienta
     * @param flashcardIdMapping   mapa lokalnych id na id serwera
     * @return opis konfliktu lub null jesli brak konfliktu
     * @throws ResourceAccessDeniedException gdy fiszka nalezy do talii innego uzytkownika
     */
    private String processFlashcard(SyncFlashcardDTO dto, User owner, LocalDateTime clientTimestamp, Map<Long, Long> flashcardIdMapping) {
        if (dto.getId() == null) {
            // Nowa fiszka
            if (dto.getDeckId() == null) {
                return "Flashcard conflict: missing deckId for new flashcard";
            }
            Deck deck = deckRepository.findById(dto.getDeckId()).orElse(null);
            if (deck == null || !deck.getOwner().getId().equals(owner.getId())) {
                return "Flashcard conflict: deck not found or access denied for deckId=" + dto.getDeckId();
            }
            Flashcard flashcard = Flashcard.builder()
                    .deck(deck)
                    .question(dto.getQuestion())
                    .answer(dto.getAnswer())
                    .build();
            flashcard = flashcardRepository.save(flashcard);
            if (dto.getLocalId() != null) {
                flashcardIdMapping.put(dto.getLocalId(), flashcard.getId());
            }
            return null;
        }

        return flashcardRepository.findById(dto.getId()).map(existing -> {
            if (!existing.getDeck().getOwner().getId().equals(owner.getId())) {
                throw new ResourceAccessDeniedException(
                        "Access denied: flashcard id=" + dto.getId() + " belongs to another user");
            }

            if (existing.getUpdatedAt() != null &&
                    existing.getUpdatedAt().isAfter(clientTimestamp)) {
                // Konflikt — last-write-wins
                if (dto.getUpdatedAt() != null && dto.getUpdatedAt().isAfter(existing.getUpdatedAt())) {
                    existing.setQuestion(dto.getQuestion());
                    existing.setAnswer(dto.getAnswer());
                    flashcardRepository.save(existing);
                    return "Flashcard conflict (client-wins): id=" + dto.getId();
                }
                return "Flashcard conflict (server-wins): id=" + dto.getId();
            }

            existing.setQuestion(dto.getQuestion());
            existing.setAnswer(dto.getAnswer());
            flashcardRepository.save(existing);
            return null;
        }).orElse(null);
    }
}
