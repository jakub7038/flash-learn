package com.flashlearn.backend.deck;

import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock DeckRepository deckRepository;
    @Mock UserRepository userRepository;
    @Mock SecurityContext securityContext;
    @Mock Authentication authentication;

    @InjectMocks DeckService deckService;

    private User owner;
    private User otherUser;
    private Deck deck;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.com").build();
        otherUser = User.builder().id(2L).email("other@test.com").build();

        deck = Deck.builder()
                .id(10L)
                .owner(owner)
                .title("Test Deck")
                .description("desc")
                .isPublic(false)
                .build();

        when(authentication.getName()).thenReturn("owner@test.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsDecksForCurrentUser() {
        when(deckRepository.findByOwnerId(1L)).thenReturn(List.of(deck));

        List<DeckResponse> result = deckService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Deck");
    }

    @Test
    void getAll_noDecks_returnsEmptyList() {
        when(deckRepository.findByOwnerId(1L)).thenReturn(List.of());

        List<DeckResponse> result = deckService.getAll();

        assertThat(result).isEmpty();
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_ownDeck_returnsDeck() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));

        DeckResponse result = deckService.getById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Test Deck");
    }

    @Test
    void getById_notFound_throwsDeckNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.getById(99L))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void getById_otherUsersDeck_throwsResourceAccessDeniedException() {
        Deck otherDeck = Deck.builder().id(20L).owner(otherUser).title("Other").build();
        when(deckRepository.findById(20L)).thenReturn(Optional.of(otherDeck));

        assertThatThrownBy(() -> deckService.getById(20L))
                .isInstanceOf(ResourceAccessDeniedException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesDeckAndReturnsResponse() {
        DeckRequest request = new DeckRequest();
        request.setTitle("New Deck");
        request.setDescription("New desc");
        request.setPublic(false);

        when(deckRepository.save(any(Deck.class))).thenReturn(deck);

        DeckResponse result = deckService.create(request);

        assertThat(result.getTitle()).isEqualTo("Test Deck");
        verify(deckRepository).save(any(Deck.class));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_ownDeck_updatesAndReturnsResponse() {
        DeckRequest request = new DeckRequest();
        request.setTitle("Updated");
        request.setDescription("Updated desc");
        request.setPublic(true);

        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenReturn(deck);

        DeckResponse result = deckService.update(10L, request);

        assertThat(result).isNotNull();
        verify(deckRepository).save(deck);
    }

    @Test
    void update_notFound_throwsDeckNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        DeckRequest request = new DeckRequest();
        request.setTitle("X");

        assertThatThrownBy(() -> deckService.update(99L, request))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void update_otherUsersDeck_throwsResourceAccessDeniedException() {
        Deck otherDeck = Deck.builder().id(20L).owner(otherUser).title("Other").build();
        when(deckRepository.findById(20L)).thenReturn(Optional.of(otherDeck));

        DeckRequest request = new DeckRequest();
        request.setTitle("Stolen");

        assertThatThrownBy(() -> deckService.update(20L, request))
                .isInstanceOf(ResourceAccessDeniedException.class);
        verify(deckRepository, never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_ownDeck_deletesDeck() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));

        deckService.delete(10L);

        verify(deckRepository).delete(deck);
    }

    @Test
    void delete_notFound_throwsDeckNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.delete(99L))
                .isInstanceOf(DeckNotFoundException.class);
        verify(deckRepository, never()).delete(any());
    }

    @Test
    void delete_otherUsersDeck_throwsResourceAccessDeniedException() {
        Deck otherDeck = Deck.builder().id(20L).owner(otherUser).title("Other").build();
        when(deckRepository.findById(20L)).thenReturn(Optional.of(otherDeck));

        assertThatThrownBy(() -> deckService.delete(20L))
                .isInstanceOf(ResourceAccessDeniedException.class);
        verify(deckRepository, never()).delete(any());
    }
}
