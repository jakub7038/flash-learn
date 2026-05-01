package com.flashlearn.backend.repository;

import com.flashlearn.backend.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserId(Long userId);

    /**
     * Pobiera sesje użytkownika zakończone po podanej dacie.
     * Używane do agregacji statystyk za ostatnie 7/30 dni.
     */
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId AND s.finishedAt >= :since")
    List<StudySession> findByUserIdAndFinishedAtAfter(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Zwraca unikalne dni nauki użytkownika w podanym przedziale.
     * Używane do obliczenia streaku.
     */
    @Query("SELECT DISTINCT CAST(s.finishedAt AS date) FROM StudySession s " +
            "WHERE s.user.id = :userId AND s.finishedAt >= :since " +
            "ORDER BY CAST(s.finishedAt AS date)")
    List<LocalDate> findDistinctStudyDays(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Zwraca rozkład ocen dla użytkownika w podanym przedziale.
     * Wynik: [rating, count]
     */
    @Query("SELECT r.rating, COUNT(r) FROM StudySessionResult r " +
            "WHERE r.session.user.id = :userId AND r.session.finishedAt >= :since " +
            "GROUP BY r.rating")
    List<Object[]> findRatingDistribution(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Zlicza fiszki uczone per dzień w podanym przedziale.
     * Wynik: [date, count]
     */
    @Query("SELECT CAST(r.reviewedAt AS date), COUNT(r) FROM StudySessionResult r " +
            "WHERE r.session.user.id = :userId AND r.reviewedAt >= :since " +
            "GROUP BY CAST(r.reviewedAt AS date) " +
            "ORDER BY CAST(r.reviewedAt AS date)")
    List<Object[]> findCardsPerDay(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);
}