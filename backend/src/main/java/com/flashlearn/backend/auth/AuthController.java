package com.flashlearn.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autentykacja", description = "Rejestracja i logowanie użytkowników")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Rejestracja nowego użytkownika")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Użytkownik zarejestrowany"),
        @ApiResponse(responseCode = "409", description = "Email już zajęty"),
        @ApiResponse(responseCode = "400", description = "Błąd walidacji")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Logowanie użytkownika")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Zalogowano, zwraca tokeny JWT"),
        @ApiResponse(responseCode = "401", description = "Nieprawidłowe dane logowania")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Szybkie logowanie jako test user",
               description = "Tworzy konto test@test.com (jeśli nie istnieje) i zwraca tokeny")
    @ApiResponse(responseCode = "200", description = "Zalogowano jako test user")
    @PostMapping("/test-login")
    public ResponseEntity<LoginResponse> testLogin() {
        String testEmail = "test@test.com";
        String testPassword = "haslo123";

        try {
            RegisterRequest registerReq = new RegisterRequest();
            registerReq.setEmail(testEmail);
            registerReq.setPassword(testPassword);
            authService.register(registerReq);
        } catch (Exception ignored) {
        }

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testEmail);
        loginReq.setPassword(testPassword);
        return ResponseEntity.ok(authService.login(loginReq));
    }
}
