package com.flashlearn.backend.stats;

import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.StudySessionRepository;
import com.flashlearn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serwis agregujący statystyki nauki użytkownika.
 * Oblicza streak, rozkład ocen i liczbę fiszek per dzień.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;

    /**
     * Zwraca statystyki nauki zalogowanego użytkownika.
     * Dane agregowane za ostatnie 7 i 30 dni.
     *
     * @return statystyki użytkownika
     */
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime since7  = now.minusDays(7);
        LocalDateTime since30 = now.minusDays(30);

        // Fiszki per dzień
        Map<String, Long> cardsPerDay7  = buildCardsPerDay(user.getId(), since7);
        Map<String, Long> cardsPerDay30 = buildCardsPerDay(user.getId(), since30);

        // Rozkład ocen — 30 dni
        long wrong = 0, hard = 0, correct = 0;
        List<Object[]> ratingDist = studySessionRepository.findRatingDistribution(user.getId(), since30);
        for (Object[] row : ratingDist) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (rating == 0)      wrong   = count;
            else if (rating == 1) hard    = count;
            else if (rating == 2) correct = count;
        }
        long total = wrong + hard + correct;

        // Streak — unikalne dni nauki w ostatnich 30 dniach
        List<LocalDate> studyDays = studySessionRepository.findStudySessionDates(user.getId(), since30)
                .stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .toList();
        int currentStreak = calculateCurrentStreak(studyDays);
        int longestStreak = calculateLongestStreak(studyDays);

        return new StatsResponse(
                currentStreak, longestStreak,
                cardsPerDay7, cardsPerDay30,
                wrong, hard, correct, total
        );
    }

    /**
     * Buduje mapę liczby fiszek per dzień od podanej daty.
     *
     * @param userId identyfikator użytkownika
     * @param since  data początkowa
     * @return mapa: data (yyyy-MM-dd) → liczba fiszek
     */
    private Map<String, Long> buildCardsPerDay(Long userId, LocalDateTime since) {
        List<Object[]> rows = studySessionRepository.findCardsPerDay(userId, since);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            // Unikanie ClassCastException na java.sql.Date
            String dateStr = row[0].toString();
            Long count     = ((Number) row[1]).longValue();
            result.put(dateStr, count);
        }
        return result;
    }

    /**
     * Oblicza aktualny streak (ile dni z rzędu licząc wstecz od dziś).
     *
     * @param studyDays posortowana lista dni nauki
     * @return długość aktualnego streaku
     */
    private int calculateCurrentStreak(List<LocalDate> studyDays) {
        if (studyDays.isEmpty()) return 0;

        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Streak liczymy tylko jeśli użytkownik uczył się dziś lub wczoraj
        LocalDate last = studyDays.get(studyDays.size() - 1);
        if (!last.equals(today) && !last.equals(yesterday)) return 0;

        int streak = 1;
        for (int i = studyDays.size() - 2; i >= 0; i--) {
            if (studyDays.get(i).equals(studyDays.get(i + 1).minusDays(1))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * Oblicza najdłuższy streak w podanej liście dni nauki.
     *
     * @param studyDays posortowana lista dni nauki
     * @return długość najdłuższego streaku
     */
    private int calculateLongestStreak(List<LocalDate> studyDays) {
        if (studyDays.isEmpty()) return 0;

        int longest = 1;
        int current = 1;

        for (int i = 1; i < studyDays.size(); i++) {
            if (studyDays.get(i).equals(studyDays.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}