package com.flashlearn.backend.progress;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sync/progress")
@RequiredArgsConstructor
@Tag(name = "Postęp SM-2", description = "Postęp nauki fiszek algorytmem SM-2")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    @Operation(summary = "Pobierz postęp SM-2",
            description = "Zwraca stan nextReview, easeFactor i intervalDays dla fiszek użytkownika. " +
                    "Parametr dueOnly=true zwraca tylko fiszki do powtórki dzisiaj lub wcześniej.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista postępów SM-2"),
            @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    @GetMapping
    public ResponseEntity<List<ProgressResponse>> getProgress(
            @RequestParam(defaultValue = "false") boolean dueOnly) {

        List<ProgressResponse> result = dueOnly
                ? progressService.getDueToday()
                : progressService.getAll();

        return ResponseEntity.ok(result);
    }
}