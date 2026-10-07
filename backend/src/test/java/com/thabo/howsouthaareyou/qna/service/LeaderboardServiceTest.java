package com.thabo.howsouthaareyou.qna.service;

import com.thabo.howsouthaareyou.qna.dto.LeaderboardEntryDto;
import com.thabo.howsouthaareyou.qna.dto.LeaderboardResponse;
import com.thabo.howsouthaareyou.qna.repository.LeaderboardProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private com.thabo.howsouthaareyou.qna.repository.LeaderboardRepository leaderboardRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void getLeaderboard_surfacesAccumulatedScore() {
        UUID userId = UUID.randomUUID();
        LeaderboardProjection projection = org.mockito.Mockito.mock(LeaderboardProjection.class);
        when(projection.getUserId()).thenReturn(userId);
        when(projection.getUsername()).thenReturn("winner");
        when(projection.getProfilePictureUrl()).thenReturn("pic");
        when(projection.getScore()).thenReturn(240L);

        when(leaderboardRepository.findTopAll(any(Pageable.class)))
                .thenReturn(List.of(projection));

        LeaderboardResponse response = leaderboardService.getLeaderboard("ALL", 10);

        assertThat(response.period()).isEqualTo("ALL");
        assertThat(response.entries()).hasSize(1);
        LeaderboardEntryDto entry = response.entries().get(0);
        assertThat(entry.rank()).isEqualTo(1);
        assertThat(entry.username()).isEqualTo("winner");
        assertThat(entry.score()).isEqualTo(240L);
    }
}