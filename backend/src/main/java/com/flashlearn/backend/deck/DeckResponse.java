package com.flashlearn.backend.deck;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DeckResponse {

    private Long id;
    private String title;
    private String description;

    @JsonProperty("isPublic")
    private Boolean isPublic;

    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}