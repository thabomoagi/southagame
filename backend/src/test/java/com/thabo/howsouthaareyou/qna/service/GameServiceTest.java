package com.thabo.howsouthaareyou.qna.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.common.exception.NotFoundException;
import com.thabo.howsouthaareyou.common.exception.UnauthorizedException;
import com.thabo.howsouthaareyou.qna.dto.AttemptResultResponse;
import com.thabo.howsouthaareyou.qna.dto.StartAttemptRequest;
import com.thabo.howsouthaareyou.qna.dto.StartAttemptResponse;
import com.thabo.howsouthaareyou.qna.dto.SubmitAttemptRequest;
import com.thabo.howsouthaareyou.qna.entity.Attempt;
import com.thabo.howsouthaareyou.qna.entity.Difficulty;
import com.thabo.howsouthaareyou.qna.entity.Question;
import com.thabo.howsouthaareyou.qna.repository.AttemptAnswerRepository;
import com.thabo.howsouthaareyou.qna.repository.AttemptQuestionRepository;
import com.thabo.howsouthaareyou.qna.repository.AttemptRepository;
import com.thabo.howsouthaareyou.qna.repository.OptionRepository;
import com.thabo.howsouthaareyou.qna.repository.QuestionRepository;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.service.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private OptionRepository optionRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;
    @Mock
    private AttemptQuestionRepository attemptQuestionRepository;

    @InjectMocks
    private GameService gameService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("player")
                .email("player@example.com")
                .passwordHash("hash")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Question question(long id) {
        return Question.builder()
                .id(id)
                .prompt("Q" + id)
                .difficulty(Difficulty.EASY)
                .active(true)
                .options(new ArrayList<>())
                .build();
    }

    @Test
    void startAttempt_throws_whenDailyLimitReached() {
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.countAttemptsStartedSince(eq(user.getId()), any())).thenReturn(20L);

        assertThatThrownBy(() -> gameService.startAttempt(new StartAttemptRequest(Difficulty.EASY)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("daily limit of 20");
    }

    @Test
    void startAttempt_ok_whenUnderDailyLimit() {
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            questions.add(question(i));
        }
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.countAttemptsStartedSince(eq(user.getId()), any())).thenReturn(3L);
        when(questionRepository.findRandomActiveQuestions(any())).thenReturn(questions);

        StartAttemptResponse response = gameService.startAttempt(
                new StartAttemptRequest(Difficulty.EASY));

        assertThat(response).isNotNull();
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void submitAttempt_throws_whenAttemptNotOwned() {
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.findByIdAndUserId(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.submitAttempt(
                UUID.randomUUID(), new SubmitAttemptRequest(List.of())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void submitAttempt_throws_whenAttemptExpired() {
        UUID attemptId = UUID.randomUUID();
        Attempt expired = Attempt.builder()
                .id(attemptId)
                .user(user)
                .difficulty(Difficulty.EASY)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().minusMinutes(4))
                .score(0)
                .totalQuestions(5)
                .correctCount(0)
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.findByIdAndUserId(attemptId, user.getId()))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> gameService.submitAttempt(
                attemptId, new SubmitAttemptRequest(List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void submitAttempt_returnsResult_whenAlreadyCompleted() {
        UUID attemptId = UUID.randomUUID();
        Attempt completed = Attempt.builder()
                .id(attemptId)
                .user(user)
                .difficulty(Difficulty.EASY)
                .startedAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .completedAt(LocalDateTime.now())
                .score(4)
                .totalQuestions(5)
                .correctCount(4)
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.findByIdAndUserId(attemptId, user.getId()))
                .thenReturn(Optional.of(completed));
        when(attemptAnswerRepository.findByAttemptId(attemptId)).thenReturn(List.of());

        AttemptResultResponse result = gameService.submitAttempt(
                attemptId, new SubmitAttemptRequest(List.of()));

        assertThat(result.score()).isEqualTo(4);
        assertThat(result.correctCount()).isEqualTo(4);
        verify(attemptAnswerRepository).findByAttemptId(attemptId);
    }

    @Test
    void getAttemptResult_throws_whenAttemptNotOwned() {
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(attemptRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.getAttemptResult(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void startAttempt_rejectsUnauthenticatedUser() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new UnauthorizedException("User not authenticated"));

        assertThatThrownBy(() -> gameService.startAttempt(new StartAttemptRequest(Difficulty.EASY)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void submitAttempt_rejectsUnauthenticatedUser() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new UnauthorizedException("User not authenticated"));

        assertThatThrownBy(() -> gameService.submitAttempt(
                UUID.randomUUID(), new SubmitAttemptRequest(List.of())))
                .isInstanceOf(UnauthorizedException.class);
    }
}