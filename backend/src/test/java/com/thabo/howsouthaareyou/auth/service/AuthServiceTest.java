package com.thabo.howsouthaareyou.auth.service;

import com.thabo.howsouthaareyou.auth.dto.AuthResponse;
import com.thabo.howsouthaareyou.auth.dto.LoginRequest;
import com.thabo.howsouthaareyou.auth.dto.RefreshTokenRequest;
import com.thabo.howsouthaareyou.auth.dto.RegisterRequest;
import com.thabo.howsouthaareyou.auth.entity.PasswordResetToken;
import com.thabo.howsouthaareyou.auth.entity.RefreshToken;
import com.thabo.howsouthaareyou.auth.repository.PasswordResetTokenRepository;
import com.thabo.howsouthaareyou.auth.repository.RefreshTokenRepository;
import com.thabo.howsouthaareyou.auth.security.JwtService;
import com.thabo.howsouthaareyou.auth.security.LoginAttemptService;
import com.thabo.howsouthaareyou.common.exception.ConflictException;
import com.thabo.howsouthaareyou.common.exception.UnauthorizedException;
import com.thabo.howsouthaareyou.email.EmailService;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Pattern HASH_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private static RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static User user(UUID id, String username, String email, String password) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .passwordHash(password)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private static RefreshTokenRequest refreshRequest(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token);
        return request;
    }

    // ---------- Registration & duplicate checks ----------

    @Test
    void register_throwsConflict_whenUsernameTaken() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);
        RegisterRequest request = registerRequest("taken", "user@example.com", "s3cret1way");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username is already taken");
    }

    @Test
    void register_throwsConflict_whenEmailTaken() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        RegisterRequest request = registerRequest("fresh", "user@example.com", "s3cret1way");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void register_encodesPassword_andReturnsTokens() {
        when(userRepository.existsByUsername("fresh")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cret1way")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.register(
                registerRequest("fresh", "user@example.com", "s3cret1way"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    // ---------- Login ----------

    @Test
    void login_resetsAttempts_onValidCredentials() {
        UUID id = UUID.randomUUID();
        User logUser = user(id, "fresh", "user@example.com", "hash");
        when(loginAttemptService.isBlocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(logUser));
        when(passwordEncoder.matches("good1pass", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(logUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(logUser)).thenReturn("refresh");
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("user@example.com");
        request.setPassword("good1pass");

        AuthResponse response = authService.login(request);

        verify(loginAttemptService).loginSucceeded("user@example.com");
        assertThat(response.getUserId()).isEqualTo(id);
    }

    @Test
    void login_recordsFailure_onWrongPassword() {
        User logUser = user(UUID.randomUUID(), "uname", "user@example.com", "hash");
        when(loginAttemptService.isBlocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(logUser));
        when(passwordEncoder.matches("badpass1", "hash")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("user@example.com");
        request.setPassword("badpass1");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
        verify(loginAttemptService).loginFailed("user@example.com");
    }

    @Test
    void login_throwsTooMany_whenLocked() {
        when(loginAttemptService.isBlocked("user@example.com")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("user@example.com");
        request.setPassword("what1ever");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ---------- Refresh token rotation & reuse detection ----------

    @Test
    void refresh_rotates_byRevokingPreviousToken() {
        User refUser = user(UUID.randomUUID(), "fresh", "user@example.com", "hash");
        RefreshToken stored = RefreshToken.builder()
                .user(refUser)
                .tokenHash("old-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .revoked(false)
                .build();

        when(jwtService.isRefreshTokenValid("raw-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(refUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(refUser)).thenReturn("new-refresh");
        when(jwtService.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.refresh(refreshRequest("raw-token"));

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_revokesAll_whenReusedTokenPresented() {
        User refUser = user(UUID.randomUUID(), "fresh", "user@example.com", "hash");
        RefreshToken replayed = RefreshToken.builder()
                .user(refUser)
                .tokenHash("revoked-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .revoked(true)
                .build();

        when(jwtService.isRefreshTokenValid("raw-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(replayed));

        assertThatThrownBy(() -> authService.refresh(refreshRequest("raw-token")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Refresh token reuse detected. All sessions have been revoked.");

        verify(refreshTokenRepository).revokeAllByUserId(refUser.getId());
    }

    @Test
    void refresh_throws_whenTokenUnrecognized() {
        when(jwtService.isRefreshTokenValid("raw-token")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshRequest("raw-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_deletesMatchingToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(
                RefreshToken.builder().tokenHash("h").revoked(false).build()));

        authService.logout(refreshRequest("raw-token"));

        verify(refreshTokenRepository).delete(any(RefreshToken.class));
    }

    // ---------- Forgot / reset password ----------

    @Test
    void requestPasswordReset_isSilent_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("ghost@example.com");

        verify(emailService, never()).sendPasswordResetEmail(anyString(), any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_storesHashedToken_andEmailsRawToken() {
        User resetUser = user(UUID.randomUUID(), "fresh", "user@example.com", "hash");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(resetUser));

        authService.requestPasswordReset("user@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        String stored = captor.getValue().getToken();
        assertThat(stored).matches(HASH_PATTERN);

        ArgumentCaptor<String> tokenArg = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), tokenArg.capture());
        assertThat(tokenArg.getValue()).isNotNull();
        assertThat(tokenArg.getValue()).isNotEqualTo(stored);
    }

    @Test
    void requestPasswordReset_withinInMonths_emailsNotice_andCreatesNoToken() {
        User resetUser = user(UUID.randomUUID(), "fresh", "user@example.com", "hash");
        resetUser.setLastPasswordResetAt(LocalDateTime.now().minusMonths(1));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(resetUser));

        authService.requestPasswordReset("user@example.com");

        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_rehashesPassword_andInvalidatesSessions() {
        UUID userId = UUID.randomUUID();
        User resetUser = user(userId, "fresh", "user@example.com", "old-hash");
        resetUser.setLastPasswordResetAt(LocalDateTime.now().minusMonths(4));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("hashed-token")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .user(resetUser)
                .build();
        when(passwordResetTokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("Newpass1x")).thenReturn("new-hash");

        authService.resetPassword("raw-token", "Newpass1x");

        assertThat(resetUser.getPasswordHash()).isEqualTo("new-hash");
        assertThat(resetUser.getLastPasswordResetAt()).isNotNull();
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(passwordResetTokenRepository).delete(resetToken);
    }

    @Test
    void resetPassword_throwsAndDeletes_whenExpired() {
        User user = user(UUID.randomUUID(), "fresh", "user@example.com", "hash");
        PasswordResetToken expired = PasswordResetToken.builder()
                .token("hashed-token")
                .expiryDate(LocalDateTime.now().minusMinutes(5))
                .user(user)
                .build();
        when(passwordResetTokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.resetPassword("raw-token", "Newpass1x"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(passwordResetTokenRepository).delete(expired);
    }

    @Test
    void resetPassword_throws_whenInvalidToken() {
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("raw-token", "Newpass1x"))
                .isInstanceOf(ResponseStatusException.class);
    }
}