package com.flashlearn.backend.flashcard;

import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.FlashcardNotFoundException;
import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.FlashcardRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serwis obsługujący operacje CRUD na fiszkach w kontekście talii.
 * Każda operacja weryfikuje własność talii przez zalogowanego użytkownika.
 */
@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    /**
     * Zwraca wszystkie fiszki z danej talii.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param deckId identyfikator talii
     * @return lista fiszek
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    public List<FlashcardResponse> getAll(Long deckId) {
        Deck deck = getDeckForCurrentUser(deckId);
        return flashcardRepository.findByDeckId(deck.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Zwraca pojedynczą fiszkę z danej talii.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param deckId identyfikator talii
     * @param id     identyfikator fiszki
     * @return dane fiszki
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws FlashcardNotFoundException gdy fiszka nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    public FlashcardResponse getById(Long deckId, Long id) {
        getDeckForCurrentUser(deckId);
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        if (!flashcard.getDeck().getId().equals(deckId)) {
            throw new ResourceAccessDeniedException("Flashcard id=" + id + " does not belong to deck id=" + deckId);
        }

        return toResponse(flashcard);
    }

    /**
     * Tworzy nową fiszkę w danej talii.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param deckId  identyfikator talii
     * @param request dane nowej fiszki
     * @return utworzona fiszka
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public FlashcardResponse create(Long deckId, FlashcardRequest request) {
        Deck deck = getDeckForCurrentUser(deckId);

        Flashcard flashcard = Flashcard.builder()
                .deck(deck)
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .build();

        return toResponse(flashcardRepository.save(flashcard));
    }

    /**
     * Aktualizuje istniejącą fiszkę.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param deckId  identyfikator talii
     * @param id      identyfikator fiszki
     * @param request nowe dane fiszki
     * @return zaktualizowana fiszka
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws FlashcardNotFoundException gdy fiszka nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public FlashcardResponse update(Long deckId, Long id, FlashcardRequest request) {
        getDeckForCurrentUser(deckId);
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        if (!flashcard.getDeck().getId().equals(deckId)) {
            throw new ResourceAccessDeniedException("Flashcard id=" + id + " does not belong to deck id=" + deckId);
        }

        flashcard.setQuestion(request.getQuestion());
        flashcard.setAnswer(request.getAnswer());

        return toResponse(flashcardRepository.save(flashcard));
    }

    /**
     * Usuwa fiszkę z danej talii.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param deckId identyfikator talii
     * @param id     identyfikator fiszki
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws FlashcardNotFoundException gdy fiszka nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public void delete(Long deckId, Long id) {
        getDeckForCurrentUser(deckId);
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        if (!flashcard.getDeck().getId().equals(deckId)) {
            throw new ResourceAccessDeniedException("Flashcard id=" + id + " does not belong to deck id=" + deckId);
        }

        flashcardRepository.delete(flashcard);
    }

    private Deck getDeckForCurrentUser(Long deckId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + deckId);
        }

        return deck;
    }

    private FlashcardResponse toResponse(Flashcard flashcard) {
        return new FlashcardResponse(
                flashcard.getId(),
                flashcard.getDeck().getId(),
                flashcard.getQuestion(),
                flashcard.getAnswer(),
                flashcard.getCreatedAt(),
                flashcard.getUpdatedAt()
        );
    }
}