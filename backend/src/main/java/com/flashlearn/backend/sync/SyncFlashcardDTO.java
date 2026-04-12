package com.flashlearn.backend.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SyncFlashcardDTO {

    private Long id; // null jeśli nowa fiszka

    private Long localId; // id używane lokalnie w aplikacji

    private Long deckId; // id talii nadrzędnej (z serwera)

    @NotBlank(message = "Question is required")
    @Size(max = 500, message = "Question must not exceed 500 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 500, message = "Answer must not exceed 500 characters")
    private String answer;

    @NotNull(message = "UpdatedAt is required")
    private LocalDateTime updatedAt;
}