package com.flashlearn.backend.exception;

public class FlashcardNotFoundException extends RuntimeException {
    public FlashcardNotFoundException(Long id) {
        super("Flashcard not found: id=" + id);
    }
}