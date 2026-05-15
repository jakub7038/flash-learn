package com.flashlearn.backend.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SyncDeckDTO {

    private Long id; // null jeśli nowa talia

    private Long localId; // id używane lokalnie w aplikacji do mapowania

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private boolean isPublic;

    private String categorySlug;

    @NotNull(message = "UpdatedAt is required")
    private LocalDateTime updatedAt;

    private List<SyncFlashcardDTO> flashcards;
}
