package com.flashlearn.backend.progress;

import com.flashlearn.backend.exception.UserNotFoundException;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.model.UserFlashcardProgress;
import com.flashlearn.backend.repository.UserFlashcardProgressRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Serwis zwracający postęp SM-2 użytkownika.
 * Android wywołuje po synchronizacji żeby wiedzieć które fiszki pokazać.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UserFlashcardProgressRepository progressRepository;
    private final UserRepository userRepository;

    /**
     * Zwraca postęp SM-2 dla wszystkich fiszek zalogowanego użytkownika.
     *
     * @return lista postępów per fiszka
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getAll() {
        User user = getCurrentUser();
        return progressRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Zwraca postęp SM-2 tylko dla fiszek do powtórki dzisiaj lub wcześniej.
     * Używane przez Android do zaplanowania sesji nauki.
     *
     * @return lista fiszek do powtórki
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getDueToday() {
        User user = getCurrentUser();
        return progressRepository
                .findByUserIdAndNextReviewDateLessThanEqual(user.getId(), LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Pobiera zalogowanego użytkownika z SecurityContext.
     * Rzuca wyjątek jeśli brak autentykacji lub użytkownik nie istnieje w bazie.
     *
     * @return zalogowany użytkownik
     * @throws RuntimeException gdy brak autentykacji w kontekście
     * @throws UserNotFoundException gdy użytkownik nie istnieje w bazie
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authentication found in security context");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    private ProgressResponse toResponse(UserFlashcardProgress p) {
        return new ProgressResponse(
                p.getFlashcard().getId(),
                p.getFlashcard().getDeck().getId(),
                p.getEaseFactor(),
                p.getIntervalDays(),
                p.getRepetitions(),
                p.getNextReviewDate()
        );
    }
}