package com.flashlearn.backend.sync;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronizacja", description = "Synchronizacja danych między urządzeniem a serwerem")
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final SyncService syncService;

    @Operation(summary = "Wyślij zmiany z urządzenia na serwer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zmiany przetworzone"),
            @ApiResponse(responseCode = "400", description = "Błąd walidacji"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @PostMapping("/push")
    public ResponseEntity<SyncPushResponse> push(@Valid @RequestBody SyncPushRequest request) {
        return ResponseEntity.ok(syncService.push(request));
    }

    @Operation(summary = "Pobierz zmiany z serwera na urządzenie",
               description = "Zwraca talie i fiszki zmienione po wskazanym timestamp. Wyniki są stronicowane.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dane zmienione po since"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy format parametrów"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @GetMapping("/pull")
    public ResponseEntity<SyncPullResponse> pull(
            @Parameter(description = "Pobierz zmiany od tego timestamp (ISO 8601)", example = "2026-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @Parameter(description = "Numer strony (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Liczba wyników na stronę", example = "50")
            @RequestParam(defaultValue = "50") int pageSize) {
        return ResponseEntity.ok(syncService.pull(since, page, pageSize));
    }
}