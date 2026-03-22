package com.flashlearn.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Kontroler testowy do weryfikacji działania JWT.
 * Endpoint chroniony - wymaga tokena.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<Map<String, String>> test(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
            "message", "Autoryzacja działa!",
            "user", authentication.getName()
        ));
    }
}
