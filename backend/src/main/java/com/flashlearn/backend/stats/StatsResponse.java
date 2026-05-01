package com.flashlearn.backend.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class StatsResponse {

    /** Aktualna seria kolejnych dni nauki (streak). */
    private int currentStreak;

    /** Najdłuższa seria kolejnych dni nauki w ostatnich 30 dniach. */
    private int longestStreak;

    /** Liczba fiszek uczonych per dzień — ostatnie 7 dni. Klucz: yyyy-MM-dd */
    private Map<String, Long> cardsPerDayLast7;

    /** Liczba fiszek uczonych per dzień — ostatnie 30 dni. Klucz: yyyy-MM-dd */
    private Map<String, Long> cardsPerDayLast30;

    /** Rozkład ocen w ostatnich 30 dniach. */
    private long wrongAnswers;   // rating = 0 (nie wiem)
    private long hardAnswers;    // rating = 1 (trudne)
    private long correctAnswers; // rating = 2 (łatwe)

    /** Łączna liczba fiszek ocenionych w ostatnich 30 dniach. */
    private long totalReviewed;
}