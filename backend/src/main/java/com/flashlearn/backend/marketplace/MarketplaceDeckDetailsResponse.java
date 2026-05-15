package com.flashlearn.backend.marketplace;

import com.flashlearn.backend.flashcard.FlashcardResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MarketplaceDeckDetailsResponse {
    private Long id;
    private String title;
    private String description;
    private String ownerEmail;
    private Long categoryId;
    private String categoryName;
    private String categoryIconName;
    private List<FlashcardResponse> flashcards;
    private long downloadCount;
    private LocalDateTime createdAt;
}