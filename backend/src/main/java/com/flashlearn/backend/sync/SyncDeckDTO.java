package com.flashlearn.backend.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SyncDeckDTO {

    private Long id; // null jeśli nowa talia

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private boolean isPublic;

    @NotNull(message = "UpdatedAt is required")
    private LocalDateTime updatedAt;

    private List<SyncFlashcardDTO> flashcards;
}