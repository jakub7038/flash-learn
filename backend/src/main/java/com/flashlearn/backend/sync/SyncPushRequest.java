package com.flashlearn.backend.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SyncPushRequest {

    @NotNull(message = "Client timestamp is required")
    private LocalDateTime clientTimestamp;

    @Valid
    private List<SyncDeckDTO> decks;

    @Valid
    private List<SyncFlashcardDTO> flashcards;
}