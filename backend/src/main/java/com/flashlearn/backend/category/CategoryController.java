package com.flashlearn.backend.category;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Kategorie", description = "Lista kategorii talii uzywana w Marketplace i formularzu tworzenia talii")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Pobierz wszystkie kategorie",
            description = "Endpoint publiczny — nie wymaga tokena JWT. Zwraca liste kategorii z ikonami.")
    @ApiResponse(responseCode = "200", description = "Lista kategorii")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}