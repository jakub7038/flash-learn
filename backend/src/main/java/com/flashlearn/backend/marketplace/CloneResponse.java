package com.flashlearn.backend.marketplace;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CloneResponse {

    private Long deckId;
    private String title;
    private String description;
    private List<ClonedFlashcardResponse> flashcards;
}