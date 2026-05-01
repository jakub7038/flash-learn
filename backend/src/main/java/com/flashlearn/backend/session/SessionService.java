package com.flashlearn.backend.session;

import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.FlashcardNotFoundException;
import com.flashlearn.backend.exception.UserNotFoundException;
import com.flashlearn.backend.model.*;
import com.flashlearn.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Serwis obsługujący zapis sesji nauki.
 * Po zapisaniu wyników aktualizuje postęp SM-2 dla każdej fiszki.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    
    private final Sm2Calculator sm2Calculator;
    private final StudySessionRepository studySessionRepository;
    private final UserFlashcardProgressRepository progressRepository;
    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    /**
     * Zapisuje sesję nauki i aktualizuje postęp SM-2 dla każdej fiszki.
     *
     * @param request dane sesji z listą ocen per fiszka
     * @return podsumowanie zapisanej sesji
     * @throws DeckNotFoundException gdy talia nie istnieje
     * @throws FlashcardNotFoundException gdy fiszka nie istnieje
     */
    @Transactional
    public SessionResponse save(SessionRequest request) {
        Authentication authentication = 
        SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
        throw new RuntimeException("No authentication in context");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Deck deck = deckRepository.findById(request.getDeckId())
                .orElseThrow(() -> new DeckNotFoundException(request.getDeckId()));

        // Utwórz sesję
        StudySession session = StudySession.builder()
                .user(user)
                .deck(deck)
                .startedAt(request.getStartedAt())
                .finishedAt(request.getFinishedAt())
                .cardsReviewed(request.getResults().size())
                .build();

        // Utwórz wyniki i aktualizuj postęp SM-2
        List<StudySessionResult> results = new ArrayList<>();
        int correct = 0, hard = 0, wrong = 0;

        for (SessionResultRequest resultReq : request.getResults()) {
            Flashcard flashcard = flashcardRepository.findById(resultReq.getFlashcardId())
                    .orElseThrow(() -> new FlashcardNotFoundException(resultReq.getFlashcardId()));

            StudySessionResult result = StudySessionResult.builder()
                    .session(session)
                    .flashcard(flashcard)
                    .rating(resultReq.getRating())
                    .build();
            results.add(result);

            // Zlicz odpowiedzi
            if (resultReq.getRating() == 2) correct++;
            else if (resultReq.getRating() == 1) hard++;
            else wrong++;

            // Aktualizuj postęp SM-2
            updateProgress(user, flashcard, resultReq.getRating());
        }

        session.setResults(results);
        StudySession saved = studySessionRepository.save(session);

        return new SessionResponse(
                saved.getId(),
                deck.getId(),
                saved.getCardsReviewed(),
                correct, hard, wrong,
                saved.getStartedAt(),
                saved.getFinishedAt()
        );
    }

    /**
     * Aktualizuje postęp SM-2 dla fiszki na podstawie oceny użytkownika.
     * Algorytm SM-2: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
     *
     * @param user      zalogowany użytkownik
     * @param flashcard fiszka której dotyczy postęp
     * @param rating    ocena: 0 = nie wiem, 1 = trudne, 2 = łatwe
     */
    private void updateProgress(User user, Flashcard flashcard, int rating) {
        UserFlashcardProgress progress = progressRepository
                .findByUserIdAndFlashcardId(user.getId(), flashcard.getId())
                .orElseGet(() -> UserFlashcardProgress.builder()
                        .user(user)
                        .flashcard(flashcard)
                        .easeFactor(Sm2Calculator.DEFAULT_EASE_FACTOR)
                        .intervalDays(1)
                        .repetitions(0)
                        .nextReviewDate(LocalDate.now())
                        .build());

         // mapowanie rating (0/1/2) → quality SM-2 (0–5)
        int quality = switch (rating) {
            case 0 -> 1;  // nie wiem   → quality 1 (prawie blackout)
            case 1 -> 3;  // trudne     → quality 3 (poprawne z trudnością)
            case 2 -> 5;  // łatwe      → quality 5 (idealne)
            default -> throw new IllegalArgumentException("Invalid rating: " + rating);
        };

        Sm2Calculator.Sm2Result result = sm2Calculator.calculate(
            quality,
            progress.getEaseFactor(),   // EF z bazy — sanityzowany wewnątrz
            progress.getIntervalDays(),
            progress.getRepetitions()
        );

        progress.setEaseFactor(result.easeFactor());
        progress.setIntervalDays(result.intervalDays());
        progress.setRepetitions(result.repetitions());
        progress.setNextReviewDate(result.nextReviewDate());

        progressRepository.save(progress);
    }
}