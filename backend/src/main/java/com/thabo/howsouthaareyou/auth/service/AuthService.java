package com.thabo.howsouthaareyou.auth.service;

import com.thabo.howsouthaareyou.auth.dto.AuthResponse;
import com.thabo.howsouthaareyou.auth.dto.LoginRequest;
import com.thabo.howsouthaareyou.auth.dto.RefreshTokenRequest;
import com.thabo.howsouthaareyou.auth.dto.RegisterRequest;
import com.thabo.howsouthaareyou.auth.entity.PasswordResetToken;
import com.thabo.howsouthaareyou.auth.entity.RefreshToken;
import com.thabo.howsouthaareyou.auth.repository.PasswordResetTokenRepository;
import com.thabo.howsouthaareyou.auth.repository.RefreshTokenRepository;

import com.thabo.howsouthaareyou.auth.security.LoginAttemptService;

import com.thabo.howsouthaareyou.auth.security.JwtService;
import com.thabo.howsouthaareyou.common.exception.ConflictException;
import com.thabo.howsouthaareyou.common.exception.UnauthorizedException;
import com.thabo.howsouthaareyou.email.EmailService;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {

        if (loginAttemptService.isBlocked(request.getIdentifier())) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Account temporarily locked due to too many failed attempts.");
        }

        User user = findUserByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.loginFailed(request.getIdentifier());
            throw new UnauthorizedException("Invalid credentials");
        }

        loginAttemptService.loginSucceeded(request.getIdentifier());

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtService.isRefreshTokenValid(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not recognized"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        if (storedToken.isRevoked()) {
            // A revoked token being presented again means it was replayed after
            // rotation. Treat this as theft: revoke every session for this user.
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            throw new UnauthorizedException(
                    "Refresh token reuse detected. All sessions have been revoked.");
        }

        User user = storedToken.getUser();

        // Rotate: revoke the current token before issuing a replacement so that
        // replay of this token can be detected on a subsequent attempt.
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return buildAuthResponse(user);
    }

    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    public void requestPasswordReset(String identifier) {
        Optional<User> optionalUser = findUserByIdentifier(identifier);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        // Check if user has reset password within the last 3 months
        if (user.getLastPasswordResetAt() != null) {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (user.getLastPasswordResetAt().isAfter(threeMonthsAgo)) {
                // Still within 3-month window - send email with notice but don't create token
                emailService.sendPasswordResetEmail(user.getEmail(), null);
                return;
            }
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashToken(token))
                .expiryDate(expiryDate)
                .user(user)
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired reset token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastPasswordResetAt(LocalDateTime.now());

        // Invalidate all refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());

        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshExpirationMs() / 1000))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier);
        }

        return userRepository.findByUsername(identifier);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(
                    "Unable to hash refresh token",
                    exception);
        }
    }
}