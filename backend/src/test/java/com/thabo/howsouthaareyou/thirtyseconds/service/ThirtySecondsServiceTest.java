package com.thabo.howsouthaareyou.thirtyseconds.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.thirtyseconds.dto.SubmitRoundScoreRequest;
import com.thabo.howsouthaareyou.thirtyseconds.entity.ThirtySecondsGame;
import com.thabo.howsouthaareyou.thirtyseconds.entity.ThirtySecondsMode;
import com.thabo.howsouthaareyou.thirtyseconds.entity.ThirtySecondsRound;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsCardRepository;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsGameRepository;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsRoundRepository;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.service.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirtySecondsServiceTest {

    @Mock
    private ThirtySecondsGameRepository gameRepository;
    @Mock
    private ThirtySecondsRoundRepository roundRepository;
    @Mock
    private ThirtySecondsCardRepository cardRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ThirtySecondsService service;

    private UUID gameId;
    private ThirtySecondsGame game;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("player").build();
        game = ThirtySecondsGame.builder()
                .id(gameId)
                .user(user)
                .mode(ThirtySecondsMode.SOLO)
                .playerCount(1)
                .totalScore(0)
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(gameRepository.findByIdAndUserId(gameId, user.getId())).thenReturn(Optional.of(game));
    }

    @Test
    void submitRoundScore_rejectsScoreAboveWordCount() {
        ThirtySecondsRound round = roundWithScore(null);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));

        assertThatThrownBy(() -> service.submitRoundScore(
                gameId,
                new SubmitRoundScoreRequest(1L, 4)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("number of words");

        verify(roundRepository, never()).save(any());
    }

    @Test
    void submitRoundScore_rejectsDuplicateScore() {
        ThirtySecondsRound round = roundWithScore(1);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));

        assertThatThrownBy(() -> service.submitRoundScore(
                gameId,
                new SubmitRoundScoreRequest(1L, 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been submitted");

        verify(roundRepository, never()).save(any());
    }

    private ThirtySecondsRound roundWithScore(Integer score) {
        return ThirtySecondsRound.builder()
                .id(1L)
                .game(game)
                .roundNumber(1)
                .playerName("player")
                .prompt("one, two, three")
                .score(score)
                .build();
    }
}