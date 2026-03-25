package com.flashlearn.backend.auth;

import com.flashlearn.backend.exception.EmailAlreadyExistsException;
import com.flashlearn.backend.model.RevokedToken;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.RevokedTokenRepository;
import com.flashlearn.backend.repository.UserRepository;
import com.flashlearn.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Serwis obsługujący autentykację użytkowników.
 * Realizuje rejestrację, logowanie i odświeżanie tokenów JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Rejestruje nowego użytkownika.
     * Hashuje hasło BCrypt i zapisuje w bazie.
     *
     * @param request email i hasło
     * @return dane zarejestrowanego użytkownika
     * @throws EmailAlreadyExistsException gdy email jest zajęty
     */
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);

        return new RegisterResponse(saved.getId(), saved.getEmail(), "User registered successfully");
    }

    /**
     * Loguje użytkownika i generuje tokeny JWT.
     *
     * @param request email i hasło
     * @return access token + refresh token
     * @throws BadCredentialsException gdy dane logowania są nieprawidłowe
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken  = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * Odświeża access token na podstawie refresh tokena.
     * Stosuje rotację tokenów — stary refresh token jest unieważniany,
     * Generowana jest nowa para tokenów.
     *
     * @param request refresh token do weryfikacji
     * @return nowy access token + nowy refresh token
     * @throws InvalidTokenException gdy token jest nieważny, wygasły lub na blackliście
     */
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (revokedTokenRepository.existsByToken(token)) {
            throw new InvalidTokenException("Token has been revoked");
        }

        if (!jwtService.isTokenValid(token)) {
            throw new InvalidTokenException("Token is invalid or expired");
        }

        String email = jwtService.extractEmail(token);

        // Rotacja — unieważnij stary refresh token
        revokedTokenRepository.save(RevokedToken.builder()
                .token(token)
                .expiresAt(LocalDateTime.ofInstant(
                        jwtService.extractExpiration(token).toInstant(),
                        ZoneId.systemDefault()))
                .build());

        String newAccessToken  = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }
}
