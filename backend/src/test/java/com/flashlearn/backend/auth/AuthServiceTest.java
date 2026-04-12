package com.flashlearn.backend.auth;

import com.flashlearn.backend.exception.EmailAlreadyExistsException;
import com.flashlearn.backend.model.RevokedToken;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.RevokedTokenRepository;
import com.flashlearn.backend.repository.UserRepository;
import com.flashlearn.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RevokedTokenRepository revokedTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .passwordHash("hashed")
                .build();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_savesUserAndReturnsResponse() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");

        RegisterResponse res = authService.register(req);

        assertThat(res.getEmail()).isEqualTo("user@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken("user@test.com")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user@test.com")).thenReturn("refresh-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");

        LoginResponse res = authService.login(req);

        assertThat(res.getAccessToken()).isEqualTo("access-token");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_unknownEmail_throwsBadCredentialsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@test.com");
        req.setPassword("password123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewTokens() {
        when(revokedTokenRepository.existsByToken("refresh-token")).thenReturn(false);
        when(jwtService.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtService.extractEmail("refresh-token")).thenReturn("user@test.com");
        when(jwtService.extractExpiration("refresh-token")).thenReturn(new Date(System.currentTimeMillis() + 100000));
        when(jwtService.generateAccessToken("user@test.com")).thenReturn("new-access");
        when(jwtService.generateRefreshToken("user@test.com")).thenReturn("new-refresh");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh-token");

        RefreshTokenResponse res = authService.refresh(req);

        assertThat(res.getAccessToken()).isEqualTo("new-access");
        assertThat(res.getRefreshToken()).isEqualTo("new-refresh");
        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void refresh_revokedToken_throwsInvalidTokenException() {
        when(revokedTokenRepository.existsByToken("revoked-token")).thenReturn(true);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("revoked-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_invalidToken_throwsInvalidTokenException() {
        when(revokedTokenRepository.existsByToken("bad-token")).thenReturn(false);
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("bad-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validRefreshToken_revokesToken() {
        when(jwtService.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(revokedTokenRepository.existsByToken("refresh-token")).thenReturn(false);
        when(jwtService.extractExpiration("refresh-token")).thenReturn(new Date(System.currentTimeMillis() + 100000));

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("refresh-token");

        authService.logout(req);

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    void logout_alreadyRevokedToken_throwsInvalidTokenException() {
        when(jwtService.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(revokedTokenRepository.existsByToken("refresh-token")).thenReturn(true);

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("refresh-token");

        assertThatThrownBy(() -> authService.logout(req))
                .isInstanceOf(InvalidTokenException.class);
        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void logout_accessTokenInsteadOfRefresh_throwsInvalidTokenException() {
        when(jwtService.isTokenValid("access-token")).thenReturn(true);
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("access-token");

        assertThatThrownBy(() -> authService.logout(req))
                .isInstanceOf(InvalidTokenException.class);
    }
}
