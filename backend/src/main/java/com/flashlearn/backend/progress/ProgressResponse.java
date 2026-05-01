package com.flashlearn.backend.progress;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ProgressResponse {

    private Long flashcardId;
    private Long deckId;
    private double easeFactor;
    private int intervalDays;
    private int repetitions;
    private LocalDate nextReviewDate;
}