package com.thabo.howsouthaareyou.user.service;

import com.thabo.howsouthaareyou.auth.repository.RefreshTokenRepository;
import com.thabo.howsouthaareyou.common.exception.UnauthorizedException;
import com.thabo.howsouthaareyou.qna.repository.AttemptRepository;
import com.thabo.howsouthaareyou.qna.repository.LeaderboardRepository;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsGameRepository;
import com.thabo.howsouthaareyou.user.dto.ChangePasswordRequest;
import com.thabo.howsouthaareyou.user.dto.UserStatsResponse;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private ThirtySecondsGameRepository thirtySecondsGameRepository;
    @Mock
    private LeaderboardRepository leaderboardRepository;
    @Mock
    private ProfilePictureService profilePictureService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserService userService;

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("player")
                .email("player@example.com")
                .passwordHash("old-hash")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void changePassword_throws_whenCurrentPasswordWrong() {
        User user = user();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("wrong1x", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(
                new ChangePasswordRequest("wrong1x", "Newpass1x")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void changePassword_savesNewHash_andRevokesAllSessions() {
        User user = user();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("old1pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("Newpass1x")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword(new ChangePasswordRequest("old1pass", "Newpass1x"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(userRepository).save(user);
    }

    @Test
    void getUserStats_usesCumulativeScores_andCalculatesRanks() {
        User user = user();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);

        when(attemptRepository.countByUserId(user.getId())).thenReturn(2L);
        when(thirtySecondsGameRepository.countByUserId(user.getId())).thenReturn(1L);
        when(attemptRepository.findTotalScoreSince(any(), any())).thenReturn(40);
        when(attemptRepository.findTotalScoreByUserId(user.getId())).thenReturn(120);
        when(leaderboardRepository.countPlayersAboveScoreSince(any(), any())).thenReturn(5L);
        when(leaderboardRepository.countPlayersAboveAllTimeScore(user.getId())).thenReturn(3L);

        UserStatsResponse stats = userService.getUserStats();

        assertThat(stats.totalMcqGames()).isEqualTo(2L);
        assertThat(stats.totalThirtySecondsGames()).isEqualTo(1L);
        // Cumulative (SUM) scores surface across all windows.
        assertThat(stats.dailyScore()).isEqualTo(40);
        assertThat(stats.weeklyScore()).isEqualTo(40);
        assertThat(stats.monthlyScore()).isEqualTo(40);
        assertThat(stats.allTimeScore()).isEqualTo(120);
        // Ranks are inferred from accumulated totals (playersAbove + 1).
        assertThat(stats.dailyRank()).isEqualTo(6);
        assertThat(stats.allTimeRank()).isEqualTo(4);
    }

    @Test
    void updateProfilePicture_storesImage_updatesUser_andReturnsUrl() {
        User user = user();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(profilePictureService.store(file, user.getId()))
                .thenReturn("/uploads/user_" + user.getId() + "_123.webp");
        when(userRepository.save(user)).thenReturn(user);

        String url = userService.updateProfilePicture(file);

        assertThat(url).isEqualTo("/uploads/user_" + user.getId() + "_123.webp");
        assertThat(user.getProfilePictureUrl()).isEqualTo(url);
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(profilePictureService).store(file, user.getId());
    }
}