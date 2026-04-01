package com.flashlearn.backend.deck;

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
@RequestMapping("/decks")
@RequiredArgsConstructor
@Tag(name = "Talie", description = "Zarządzanie taliami fiszek")
@SecurityRequirement(name = "bearerAuth")
public class DeckController {

    private final DeckService deckService;

    @Operation(summary = "Pobierz wszystkie talie użytkownika")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista talii"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @GetMapping
    public ResponseEntity<List<DeckResponse>> getAll() {
        return ResponseEntity.ok(deckService.getAll());
    }

    @Operation(summary = "Pobierz talię po id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane talii"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(deckService.getById(id));
    }

    @Operation(summary = "Utwórz nową talię")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Talia utworzona"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @PostMapping
    public ResponseEntity<DeckResponse> create(@Valid @RequestBody DeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request));
    }

    @Operation(summary = "Zaktualizuj talię")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Talia zaktualizowana"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.update(id, request));
    }

    @Operation(summary = "Usuń talię")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Talia usunięta"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Brak dostępu do talii"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deckService.delete(id);
        return ResponseEntity.noContent().build();
    }
}