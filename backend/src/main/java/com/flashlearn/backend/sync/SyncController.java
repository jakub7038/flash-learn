package com.flashlearn.backend.sync;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}