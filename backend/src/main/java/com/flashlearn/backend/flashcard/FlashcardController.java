package com.flashlearn.backend.flashcard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/decks/{deckId}/flashcards")
@RequiredArgsConstructor
@Tag(name = "Fiszki", description = "Zarządzanie fiszkami w kontekście talii")
@SecurityRequirement(name = "bearerAuth")
public class FlashcardController {

    private final FlashcardService flashcardService;

    @Operation(summary = "Pobierz wszystkie fiszki z talii")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista fiszek"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @GetMapping
    public ResponseEntity<List<FlashcardResponse>> getAll(@PathVariable Long deckId) {
        return ResponseEntity.ok(flashcardService.getAll(deckId));
    }

    @Operation(summary = "Pobierz fiszkę po id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane fiszki"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii lub fiszki"),
            @ApiResponse(responseCode = "404", description = "Talia lub fiszka nie istnieje")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FlashcardResponse> getById(
            @PathVariable Long deckId,
            @PathVariable Long id) {
        return ResponseEntity.ok(flashcardService.getById(deckId, id));
    }

    @Operation(summary = "Utwórz nową fiszkę w talii")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fiszka utworzona"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @PostMapping
    public ResponseEntity<FlashcardResponse> create(
            @PathVariable Long deckId,
            @Valid @RequestBody FlashcardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flashcardService.create(deckId, request));
    }

    @Operation(summary = "Zaktualizuj fiszkę")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiszka zaktualizowana"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii lub fiszki"),
            @ApiResponse(responseCode = "404", description = "Talia lub fiszka nie istnieje")
    })
    @PutMapping("/{id}")
    public ResponseEntity<FlashcardResponse> update(
            @PathVariable Long deckId,
            @PathVariable Long id,
            @Valid @RequestBody FlashcardRequest request) {
        return ResponseEntity.ok(flashcardService.update(deckId, id, request));
    }

    @Operation(summary = "Usuń fiszkę")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fiszka usunięta"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii lub fiszki"),
            @ApiResponse(responseCode = "404", description = "Talia lub fiszka nie istnieje")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long deckId,
            @PathVariable Long id) {
        flashcardService.delete(deckId, id);
        return ResponseEntity.noContent().build();
    }
}