package com.flashlearn.backend.sync;

import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis obsługujący synchronizację danych z urządzenia mobilnego do serwera.
 * Strategia rozwiązywania konfliktów: server-wins — dane serwera mają priorytet
 * gdy obie strony zmodyfikowały ten sam rekord.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;

    /**
     * Przetwarza zmiany przesłane z urządzenia mobilnego.
     * Dla każdej talii i fiszki stosuje strategię server-wins przy konflikcie.
     *
     * @param request lista zmian z urządzenia wraz z timestamp klienta
     * @return podsumowanie przetworzonych zmian i lista konfliktów
     * @throws ResourceAccessDeniedException gdy użytkownik próbuje modyfikować cudze zasoby
     */
    @Transactional
    public SyncPushResponse push(SyncPushRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> conflicts = new ArrayList<>();
        int decksProcessed = 0;
        int flashcardsProcessed = 0;

        // Przetwarzanie talii
        if (request.getDecks() != null) {
            for (SyncDeckDTO dto : request.getDecks()) {
                String conflict = processDeck(dto, user, request.getClientTimestamp());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                decksProcessed++;
            }
        }

        // Przetwarzanie fiszek
        if (request.getFlashcards() != null) {
            for (SyncFlashcardDTO dto : request.getFlashcards()) {
                String conflict = processFlashcard(dto, user, request.getClientTimestamp());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
                flashcardsProcessed++;
            }
        }

        return new SyncPushResponse(decksProcessed, flashcardsProcessed, conflicts, LocalDateTime.now());
    }

    /**
     * Pobiera dane użytkownika zmienione po wskazanym timestamp.
     * Wyniki są stronicowane.
     *
     * @param since    timestamp od którego pobieramy zmiany
     * @param page     numer strony (0-based)
     * @param pageSize liczba wyników na stronę
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
            dto.setUpdatedAt(deck.getUpdatedAt());
            dto.setFlashcards(List.of());
            return dto;
        }).collect(Collectors.toList());

        List<SyncFlashcardDTO> flashcardDTOs = flashcardPage.getContent().stream().map(f -> {
            SyncFlashcardDTO dto = new SyncFlashcardDTO();
            dto.setId(f.getId());
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
     * Przetwarza pojedynczą talię — tworzy nową lub aktualizuje istniejącą.
     * Przy konflikcie (obie strony edytowały) wygrywa serwer.
     *
     * @param dto             dane talii z urządzenia
     * @param owner           zalogowany użytkownik
     * @param clientTimestamp timestamp ostatniej synchronizacji klienta
     * @return opis konfliktu lub null jeśli brak konfliktu
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    private String processDeck(SyncDeckDTO dto, User owner, LocalDateTime clientTimestamp) {
        if (dto.getId() == null) {
            // Nowa talia — zapisz
            Deck deck = Deck.builder()
                    .owner(owner)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .isPublic(dto.isPublic())
                    .build();
            deckRepository.save(deck);
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
                // Konflikt — serwer był edytowany po ostatnim sync klienta — server-wins
                return "Deck conflict (server-wins): id=" + dto.getId();
            }

            // Brak konfliktu — aktualizuj
            existing.setTitle(dto.getTitle());
            existing.setDescription(dto.getDescription());
            existing.setPublic(dto.isPublic());
            deckRepository.save(existing);
            return null;
        }).orElse(null);
    }

    /**
     * Przetwarza pojedynczą fiszkę — tworzy nową lub aktualizuje istniejącą.
     * Przy konflikcie wygrywa serwer.
     *
     * @param dto             dane fiszki z urządzenia
     * @param owner           zalogowany użytkownik
     * @param clientTimestamp timestamp ostatniej synchronizacji klienta
     * @return opis konfliktu lub null jeśli brak konfliktu
     * @throws ResourceAccessDeniedException gdy fiszka należy do talii innego użytkownika
     */
    private String processFlashcard(SyncFlashcardDTO dto, User owner, LocalDateTime clientTimestamp) {
        if (dto.getId() == null) {
            // Nowa fiszka bez powiązanej talii — pomijamy (talia musi być najpierw zsynchronizowana)
            return null;
        }

        return flashcardRepository.findById(dto.getId()).map(existing -> {
            if (!existing.getDeck().getOwner().getId().equals(owner.getId())) {
                throw new ResourceAccessDeniedException(
                        "Access denied: flashcard id=" + dto.getId() + " belongs to another user");
            }

            if (existing.getUpdatedAt() != null &&
                    existing.getUpdatedAt().isAfter(clientTimestamp)) {
                // Konflikt — server-wins
                return "Flashcard conflict (server-wins): id=" + dto.getId();
            }

            existing.setQuestion(dto.getQuestion());
            existing.setAnswer(dto.getAnswer());
            flashcardRepository.save(existing);
            return null;
        }).orElse(null);
    }
}