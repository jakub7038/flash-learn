package com.flashlearn.backend.flashcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashcardRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 500, message = "Question must not exceed 500 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 500, message = "Answer must not exceed 500 characters")
    private String answer;
}