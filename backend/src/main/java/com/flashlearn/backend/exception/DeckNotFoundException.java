package com.flashlearn.backend.exception;

public class DeckNotFoundException extends RuntimeException {
    public DeckNotFoundException(Long id) {
        super("Deck not found: id=" + id);
    }
}