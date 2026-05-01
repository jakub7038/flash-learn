package com.flashlearn.backend.stats;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Statystyki", description = "Agregacja statystyk nauki użytkownika")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Pobierz statystyki nauki",
            description = "Zwraca streak, rozkład ocen i liczbę fiszek per dzień za ostatnie 7 i 30 dni.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statystyki użytkownika"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @GetMapping
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }
}