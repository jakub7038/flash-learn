package com.flashlearn.backend.flashcard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FlashcardResponse {

    private Long id;
    private Long deckId;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}