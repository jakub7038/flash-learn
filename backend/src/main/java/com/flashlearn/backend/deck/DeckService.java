package com.flashlearn.backend.deck;

import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serwis obsługujący operacje CRUD na taliach fiszek.
 * Każda operacja modyfikująca dane weryfikuje własność zasobu.
 */
@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    /**
     * Zwraca wszystkie talie zalogowanego użytkownika.
     *
     * @return lista talii użytkownika
     */
    public List<DeckResponse> getAll() {
        User user = getCurrentUser();
        return deckRepository.findByOwnerId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Zwraca pojedynczą talię po id.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param id identyfikator talii
     * @return dane talii
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    public DeckResponse getById(Long id) {
        User user = getCurrentUser();
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + id);
        }

        return toResponse(deck);
    }

    /**
     * Tworzy nową talię dla zalogowanego użytkownika.
     *
     * @param request dane nowej talii
     * @return utworzona talia
     */
    @Transactional
    public DeckResponse create(DeckRequest request) {
        User user = getCurrentUser();

        Deck deck = Deck.builder()
                .owner(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .isPublic(request.isPublic())
                .build();

        return toResponse(deckRepository.save(deck));
    }

    /**
     * Aktualizuje istniejącą talię.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param id      identyfikator talii
     * @param request nowe dane talii
     * @return zaktualizowana talia
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public DeckResponse update(Long id, DeckRequest request) {
        User user = getCurrentUser();
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + id);
        }

        deck.setTitle(request.getTitle());
        deck.setDescription(request.getDescription());
        deck.setPublic(request.isPublic());

        return toResponse(deckRepository.save(deck));
    }

    /**
     * Usuwa talię wraz ze wszystkimi fiszkami.
     * Weryfikuje czy talia należy do zalogowanego użytkownika.
     *
     * @param id identyfikator talii
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws ResourceAccessDeniedException gdy talia należy do innego użytkownika
     */
    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + id);
        }

        deckRepository.delete(deck);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private DeckResponse toResponse(Deck deck) {
        return new DeckResponse(
                deck.getId(),
                deck.getTitle(),
                deck.getDescription(),
                deck.isPublic(),
                deck.getOwner().getId(),
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}