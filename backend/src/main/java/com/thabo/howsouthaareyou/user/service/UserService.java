package com.thabo.howsouthaareyou.user.service;

import com.thabo.howsouthaareyou.auth.repository.RefreshTokenRepository;
import com.thabo.howsouthaareyou.common.exception.ConflictException;
import com.thabo.howsouthaareyou.common.exception.UnauthorizedException;
import com.thabo.howsouthaareyou.config.UploadProperties;
import com.thabo.howsouthaareyou.qna.repository.AttemptRepository;
import com.thabo.howsouthaareyou.qna.repository.LeaderboardRepository;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsGameRepository;
import com.thabo.howsouthaareyou.user.dto.ChangePasswordRequest;
import com.thabo.howsouthaareyou.user.dto.UpdateUserRequest;
import com.thabo.howsouthaareyou.user.dto.UserResponse;
import com.thabo.howsouthaareyou.user.dto.UserStatsResponse;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AttemptRepository attemptRepository;
    private final ThirtySecondsGameRepository thirtySecondsGameRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final ProfilePictureService profilePictureService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final UploadProperties uploadProperties;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = currentUserProvider.getCurrentUser();
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (request.username() != null && !request.username().isBlank()) {
            String username = request.username().trim();

            if (!username.equals(user.getUsername())
                    && userRepository.existsByUsername(username)) {
                throw new ConflictException("Username is already taken");
            }

            user.setUsername(username);
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();

            if (!email.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(email)) {
                throw new ConflictException("Email is already in use");
            }

            user.setEmail(email);
        }

        if (request.profilePictureUrl() != null
                && !request.profilePictureUrl().isBlank()) {
            user.setProfilePictureUrl(request.profilePictureUrl().trim());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword()));

        user.setUpdatedAt(LocalDateTime.now());

        // Invalidate all existing sessions after a password change.
        refreshTokenRepository.revokeAllByUserId(user.getId());

        userRepository.save(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        User user = currentUserProvider.getCurrentUser();
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        User user = currentUserProvider.getCurrentUser();
        UUID userId = user.getId();

        long totalMcqGames = attemptRepository.countByUserId(userId);
        long totalThirtySecondsGames = thirtySecondsGameRepository.countByUserId(userId);

        long totalGamesPlayed = totalMcqGames + totalThirtySecondsGames;

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);

        Integer dailyScore = getTotalScoreSince(userId, startOfDay);
        Integer weeklyScore = getTotalScoreSince(userId, startOfWeek);
        Integer monthlyScore = getTotalScoreSince(userId, startOfMonth);
        Integer allTimeScore = attemptRepository.findTotalScoreByUserId(userId);

        Integer dailyRank = dailyScore == null || dailyScore == 0
                ? null
                : getRankSince(userId, startOfDay, dailyScore);

        Integer weeklyRank = weeklyScore == null || weeklyScore == 0
                ? null
                : getRankSince(userId, startOfWeek, weeklyScore);

        Integer monthlyRank = monthlyScore == null || monthlyScore == 0
                ? null
                : getRankSince(userId, startOfMonth, monthlyScore);

        Integer allTimeRank = allTimeScore == null || allTimeScore == 0
                ? null
                : getAllTimeRank(userId, allTimeScore);

        return new UserStatsResponse(
                totalGamesPlayed,
                totalMcqGames,
                totalThirtySecondsGames,
                dailyRank,
                dailyScore,
                weeklyRank,
                weeklyScore,
                monthlyRank,
                monthlyScore,
                allTimeRank,
                allTimeScore);
    }

    /**
     * Uploads and stores a new profile picture.
     * The browser already compresses and converts the image to WebP.
     */
    @Transactional
    public String updateProfilePicture(MultipartFile file) {
        User user = currentUserProvider.getCurrentUser();

        String oldUrl = user.getProfilePictureUrl();

        String profilePictureUrl = profilePictureService.store(file, user.getId());

        // Delete the old file only after the replacement has been stored successfully.
        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                String filename = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                Path oldFile = Paths.get(uploadProperties.getDir()).resolve(filename);
                boolean deleted = Files.deleteIfExists(oldFile);
                if (deleted) {
                    log.info("Deleted old profile picture: {}", filename);
                }
            } catch (Exception e) {
                log.warn("Could not delete old profile picture: {}", e.getMessage());
            }
        }

        user.setProfilePictureUrl(profilePictureUrl);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return profilePictureUrl;
    }

    private Integer getTotalScoreSince(
            UUID userId,
            LocalDateTime startDate) {

        Integer score = attemptRepository.findTotalScoreSince(userId, startDate);

        return (score == null || score == 0) ? null : score;
    }

    private Integer getRankSince(
            UUID userId,
            LocalDateTime startDate,
            Integer score) {

        long playersAbove = leaderboardRepository.countPlayersAboveScoreSince(
                userId,
                startDate);

        return (int) (playersAbove + 1);
    }

    private Integer getAllTimeRank(
            UUID userId,
            Integer score) {

        long playersAbove = leaderboardRepository.countPlayersAboveAllTimeScore(userId);

        return (int) (playersAbove + 1);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfilePictureUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}