package com.flashlearn.backend.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class SyncPullResponse {

    private List<SyncDeckDTO> decks;
    private List<SyncFlashcardDTO> flashcards;
    private LocalDateTime serverTimestamp;
    private int page;
    private int pageSize;
    private long totalDecks;
    private long totalFlashcards;
    private boolean hasMore;
}
