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
class FlashcardServiceTest {

    @Mock FlashcardRepository flashcardRepository;
    @Mock DeckRepository deckRepository;
    @Mock UserRepository userRepository;
    @Mock SecurityContext securityContext;
    @Mock Authentication authentication;

    @InjectMocks FlashcardService flashcardService;

    private User owner;
    private User otherUser;
    private Deck deck;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.com").build();
        otherUser = User.builder().id(2L).email("other@test.com").build();

        deck = Deck.builder().id(10L).owner(owner).title("Deck").build();

        flashcard = Flashcard.builder()
                .id(100L)
                .deck(deck)
                .question("What is Java?")
                .answer("A programming language")
                .build();

        when(authentication.getName()).thenReturn("owner@test.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_ownDeck_returnsFlashcards() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findByDeckId(10L)).thenReturn(List.of(flashcard));

        List<FlashcardResponse> result = flashcardService.getAll(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestion()).isEqualTo("What is Java?");
    }

    @Test
    void getAll_deckNotFound_throwsDeckNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.getAll(99L))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void getAll_otherUsersDeck_throwsResourceAccessDeniedException() {
        Deck otherDeck = Deck.builder().id(20L).owner(otherUser).title("Other").build();
        when(deckRepository.findById(20L)).thenReturn(Optional.of(otherDeck));

        assertThatThrownBy(() -> flashcardService.getAll(20L))
                .isInstanceOf(ResourceAccessDeniedException.class);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_validFlashcard_returnsFlashcard() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));

        FlashcardResponse result = flashcardService.getById(10L, 100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getQuestion()).isEqualTo("What is Java?");
    }

    @Test
    void getById_flashcardNotFound_throwsFlashcardNotFoundException() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.getById(10L, 999L))
                .isInstanceOf(FlashcardNotFoundException.class);
    }

    @Test
    void getById_flashcardBelongsToDifferentDeck_throwsResourceAccessDeniedException() {
        Deck anotherDeck = Deck.builder().id(10L).owner(owner).title("Another").build();
        Flashcard flashcardInOtherDeck = Flashcard.builder()
                .id(200L)
                .deck(Deck.builder().id(99L).owner(owner).title("X").build())
                .question("Q")
                .answer("A")
                .build();

        when(deckRepository.findById(10L)).thenReturn(Optional.of(anotherDeck));
        when(flashcardRepository.findById(200L)).thenReturn(Optional.of(flashcardInOtherDeck));

        assertThatThrownBy(() -> flashcardService.getById(10L, 200L))
                .isInstanceOf(ResourceAccessDeniedException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesFlashcardAndReturnsResponse() {
        FlashcardRequest request = new FlashcardRequest();
        request.setQuestion("New question?");
        request.setAnswer("New answer");

        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponse result = flashcardService.create(10L, request);

        assertThat(result).isNotNull();
        verify(flashcardRepository).save(any(Flashcard.class));
    }

    @Test
    void create_deckNotFound_throwsDeckNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        FlashcardRequest request = new FlashcardRequest();
        request.setQuestion("Q");
        request.setAnswer("A");

        assertThatThrownBy(() -> flashcardService.create(99L, request))
                .isInstanceOf(DeckNotFoundException.class);
        verify(flashcardRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validFlashcard_updatesAndReturnsResponse() {
        FlashcardRequest request = new FlashcardRequest();
        request.setQuestion("Updated?");
        request.setAnswer("Updated answer");

        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponse result = flashcardService.update(10L, 100L, request);

        assertThat(result).isNotNull();
        verify(flashcardRepository).save(flashcard);
    }

    @Test
    void update_flashcardNotFound_throwsFlashcardNotFoundException() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        FlashcardRequest request = new FlashcardRequest();
        request.setQuestion("Q");
        request.setAnswer("A");

        assertThatThrownBy(() -> flashcardService.update(10L, 999L, request))
                .isInstanceOf(FlashcardNotFoundException.class);
        verify(flashcardRepository, never()).save(any());
    }

    @Test
    void update_flashcardBelongsToDifferentDeck_throwsResourceAccessDeniedException() {
        Flashcard wrongDeckFlashcard = Flashcard.builder()
                .id(200L)
                .deck(Deck.builder().id(99L).owner(owner).build())
                .question("Q")
                .answer("A")
                .build();

        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(200L)).thenReturn(Optional.of(wrongDeckFlashcard));

        FlashcardRequest request = new FlashcardRequest();
        request.setQuestion("X");
        request.setAnswer("Y");

        assertThatThrownBy(() -> flashcardService.update(10L, 200L, request))
                .isInstanceOf(ResourceAccessDeniedException.class);
        verify(flashcardRepository, never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_validFlashcard_deletesFlashcard() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));

        flashcardService.delete(10L, 100L);

        verify(flashcardRepository).delete(flashcard);
    }

    @Test
    void delete_flashcardNotFound_throwsFlashcardNotFoundException() {
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.delete(10L, 999L))
                .isInstanceOf(FlashcardNotFoundException.class);
        verify(flashcardRepository, never()).delete(any());
    }

    @Test
    void delete_flashcardBelongsToDifferentDeck_throwsResourceAccessDeniedException() {
        Flashcard wrongDeckFlashcard = Flashcard.builder()
                .id(200L)
                .deck(Deck.builder().id(99L).owner(owner).build())
                .question("Q")
                .answer("A")
                .build();

        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(flashcardRepository.findById(200L)).thenReturn(Optional.of(wrongDeckFlashcard));

        assertThatThrownBy(() -> flashcardService.delete(10L, 200L))
                .isInstanceOf(ResourceAccessDeniedException.class);
        verify(flashcardRepository, never()).delete(any());
    }
}
