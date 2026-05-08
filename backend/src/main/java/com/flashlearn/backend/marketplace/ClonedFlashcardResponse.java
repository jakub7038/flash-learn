package com.flashlearn.backend.marketplace;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClonedFlashcardResponse {
    private Long id;
    private String question;
    private String answer;
}