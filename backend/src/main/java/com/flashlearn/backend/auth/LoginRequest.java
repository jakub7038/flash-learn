package com.flashlearn.backend.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Dane logowania")
public class LoginRequest {
    @NotBlank @Email
    @Schema(description = "Email użytkownika", example = "test@test.com")
    private String email;

    @NotBlank
    @Schema(description = "Hasło", example = "haslo123")
    private String password;
}
