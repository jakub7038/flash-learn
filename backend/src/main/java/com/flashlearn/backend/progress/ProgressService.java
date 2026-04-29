package com.flashlearn.backend.progress;

import com.flashlearn.backend.model.User;
import com.flashlearn.backend.model.UserFlashcardProgress;
import com.flashlearn.backend.repository.UserFlashcardProgressRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return progressRepository
                .findByUserIdAndNextReviewDateLessThanEqual(user.getId(), LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
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