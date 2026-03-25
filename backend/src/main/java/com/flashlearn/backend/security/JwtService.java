package com.flashlearn.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Serwis do generowania i walidacji tokenów JWT.
 * Obsługuje access token (krótki czas życia) i refresh token (długi czas życia).
 */
@Component
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS  = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE        = "type";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    /**
     * @param secret klucz do podpisywania tokenów (z application.properties)
     * @param accessExpirationMs czas życia access tokena w ms
     * @param refreshExpirationMs czas życia refresh tokena w ms
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Generuje krótkotrwały access token dla użytkownika.
     * @param email email użytkownika (subject tokena)
     * @return podpisany JWT access token
     */
    public String generateAccessToken(String email) {
        return buildToken(email, accessExpirationMs, TOKEN_TYPE_ACCESS);
    }

    /**
     * Generuje długotrwały refresh token dla użytkownika.
     * @param email email użytkownika (subject tokena)
     * @return podpisany JWT refresh token
     */
    public String generateRefreshToken(String email) {
        return buildToken(email, refreshExpirationMs, TOKEN_TYPE_REFRESH);
    }

    /**
     * Buduje token JWT z podanym czasem wygaśnięcia i typem.
     * @param subject     email użytkownika (subject tokena)
     * @param expirationMs czas życia tokena w ms
     * @param type        typ tokena: "access" lub "refresh"
     * @return podpisany JWT token
     */
    private String buildToken(String subject, long expirationMs, String type) {
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Wyciąga email użytkownika z tokena JWT.
     * @param token JWT token
     * @return email (subject) z tokena
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Wyciąga datę wygaśnięcia z tokena JWT.
     * @param token JWT token
     * @return data wygaśnięcia tokena
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Sprawdza czy token jest typu refresh.
     * @param token JWT token
     * @return true jeśli token jest refresh tokenem
     */
    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(extractClaims(token).get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sprawdza czy token jest prawidłowy (podpis + data wygaśnięcia).
     * @param token JWT token do walidacji
     * @return true jeśli token jest ważny
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wyciąga claims z tokena JWT.
     * @param token JWT token
     * @return claims zawarte w tokenie
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
