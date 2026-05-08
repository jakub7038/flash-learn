package com.flashlearn.backend.marketplace;

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

@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Publiczne talie dostępne do przeglądania i klonowania")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @Operation(summary = "Pobierz publiczne talie",
            description = "Publiczny endpoint — nie wymaga JWT. Sortowanie po popularności. " +
                    "Opcjonalne filtrowanie po kategorii (?category={slug}).")
    @ApiResponse(responseCode = "200", description = "Stronicowana lista talii")
    @GetMapping
    public ResponseEntity<MarketplacePageResponse> getDecks(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(marketplaceService.getDecks(category, page));
    }

    @Operation(summary = "Opublikuj talię w Marketplace",
            description = "Ustawia isPublic=true dla talii użytkownika. Wymaga JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Talia opublikowana"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "403", description = "Talia należy do innego użytkownika"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/publish")
    public ResponseEntity<Void> publish(@Valid @RequestBody PublishRequest request) {
        marketplaceService.publish(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sklonuj talię do swojej biblioteki",
            description = "Tworzy głęboką kopię publicznej talii wraz ze wszystkimi fiszkami. " +
                    "Inkrementuje download_count oryginału. Wymaga JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Talia sklonowana — zwraca nowe deckId z fiszkami"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje lub nie jest publiczna")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/clone")
    public ResponseEntity<CloneResponse> clone(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceService.clone(id));
    }

    @Operation(summary = "Zgłoś talię jako nieodpowiednią",
            description = "Zgłasza talię publiczną. Wymaga JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Zgłoszenie przyjęte"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji"),
            @ApiResponse(responseCode = "404", description = "Talia nie istnieje lub nie jest publiczna")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/report")
    public ResponseEntity<Void> report(@Valid @RequestBody ReportRequest request) {
        marketplaceService.report(request);
        return ResponseEntity.noContent().build();
    }
}