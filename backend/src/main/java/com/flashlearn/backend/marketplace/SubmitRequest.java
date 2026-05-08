package com.flashlearn.backend.marketplace;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitRequest {

    @NotNull(message = "Deck ID is required")
    private Long deckId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}