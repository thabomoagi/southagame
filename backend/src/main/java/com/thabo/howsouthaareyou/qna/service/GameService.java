package com.thabo.howsouthaareyou.qna.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.common.exception.NotFoundException;
import com.thabo.howsouthaareyou.qna.dto.AnswerResultDto;
import com.thabo.howsouthaareyou.qna.dto.AnswerSubmission;
import com.thabo.howsouthaareyou.qna.dto.AttemptResultResponse;
import com.thabo.howsouthaareyou.qna.dto.OptionDto;
import com.thabo.howsouthaareyou.qna.dto.QuestionDto;
import com.thabo.howsouthaareyou.qna.dto.StartAttemptRequest;
import com.thabo.howsouthaareyou.qna.dto.StartAttemptResponse;
import com.thabo.howsouthaareyou.qna.dto.SubmitAttemptRequest;
import com.thabo.howsouthaareyou.qna.entity.Attempt;
import com.thabo.howsouthaareyou.qna.entity.AttemptAnswer;
import com.thabo.howsouthaareyou.qna.entity.AttemptQuestion;
import com.thabo.howsouthaareyou.qna.entity.Difficulty;
import com.thabo.howsouthaareyou.qna.entity.Option;
import com.thabo.howsouthaareyou.qna.entity.Question;
import com.thabo.howsouthaareyou.qna.repository.AttemptAnswerRepository;
import com.thabo.howsouthaareyou.qna.repository.AttemptQuestionRepository;
import com.thabo.howsouthaareyou.qna.repository.AttemptRepository;
import com.thabo.howsouthaareyou.qna.repository.OptionRepository;
import com.thabo.howsouthaareyou.qna.repository.QuestionRepository;
import com.thabo.howsouthaareyou.user.entity.User;
import com.thabo.howsouthaareyou.user.service.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

        private static final int QUESTION_COUNT = 5;
        private static final int DURATION_SECONDS = 30;
        private static final int DAILY_ATTEMPT_LIMIT = 20;

        private final CurrentUserProvider currentUserProvider;
        private final QuestionRepository questionRepository;
        private final OptionRepository optionRepository;
        private final AttemptRepository attemptRepository;
        private final AttemptAnswerRepository attemptAnswerRepository;
        private final AttemptQuestionRepository attemptQuestionRepository;

        public StartAttemptResponse startAttempt(StartAttemptRequest request) {
                User user = getCurrentUser();

                Difficulty difficulty = request.difficulty();
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

                long attemptsStartedToday = attemptRepository.countAttemptsStartedSince(
                                user.getId(),
                                startOfDay);

                if (attemptsStartedToday >= DAILY_ATTEMPT_LIMIT) {
                        throw new BadRequestException(
                                        "You have reached the daily limit of 20 Multiple Choice games. Try again tomorrow.");
                }

                List<Question> questions = selectRandomQuestions(difficulty);

                if (questions.size() < QUESTION_COUNT) {
                        throw new BadRequestException(
                                        "Not enough questions available for " + difficulty + " difficulty");
                }

                LocalDateTime startedAt = LocalDateTime.now();
                LocalDateTime expiresAt = startedAt.plusSeconds(DURATION_SECONDS);

                Attempt attempt = Attempt.builder()
                                .user(user)
                                .difficulty(difficulty)
                                .startedAt(startedAt)
                                .expiresAt(expiresAt)
                                .totalQuestions(QUESTION_COUNT)
                                .score(0)
                                .correctCount(0)
                                .build();

                attemptRepository.save(attempt);
                UUID attemptId = attempt.getId();

                List<AttemptQuestion> attemptQuestions = new ArrayList<>();

                for (int i = 0; i < questions.size(); i++) {
                        attemptQuestions.add(
                                        AttemptQuestion.builder()
                                                        .attempt(attempt)
                                                        .question(questions.get(i))
                                                        .position(i)
                                                        .build());
                }

                attemptQuestionRepository.saveAll(attemptQuestions);

                List<QuestionDto> questionDtos = questions.stream()
                                .map(this::toQuestionDto)
                                .toList();

                return new StartAttemptResponse(
                                attemptId,
                                startedAt,
                                expiresAt,
                                DURATION_SECONDS,
                                QUESTION_COUNT,
                                questionDtos);
        }

        public AttemptResultResponse submitAttempt(
                        UUID attemptId,
                        SubmitAttemptRequest request) {

                User user = getCurrentUser();

                Attempt attempt = attemptRepository
                                .findByIdAndUserId(attemptId, user.getId())
                                .orElseThrow(() -> new NotFoundException("Attempt not found"));

                if (attempt.getCompletedAt() != null) {
                        return getAttemptResult(attemptId);
                }

                if (LocalDateTime.now().isAfter(attempt.getExpiresAt())) {
                        throw new BadRequestException(
                                        "This attempt has expired. Please start a new game.");
                }

                Set<Long> allowedQuestionIds = attemptQuestionRepository
                                .findByAttemptIdOrderByPositionAsc(attemptId)
                                .stream()
                                .map(attemptQuestion -> attemptQuestion.getQuestion().getId())
                                .collect(Collectors.toSet());

                Set<Long> answeredQuestionIds = new HashSet<>();
                List<AnswerResultDto> results = new ArrayList<>();

                int score = 0;
                int correctCount = 0;

                for (AnswerSubmission submission : request.answers()) {

                        if (!answeredQuestionIds.add(submission.questionId())) {
                                throw new BadRequestException(
                                                "Duplicate answer submitted");
                        }

                        if (!allowedQuestionIds.contains(submission.questionId())) {
                                throw new BadRequestException(
                                                "Question not part of this attempt");
                        }

                        Question question = questionRepository
                                        .findById(submission.questionId())
                                        .orElseThrow(() -> new NotFoundException("Question not found"));

                        Option selectedOption = null;
                        boolean correct = false;
                        int pointsEarned = 0;

                        if (submission.selectedOptionId() != null) {

                                selectedOption = optionRepository
                                                .findByIdAndQuestionId(
                                                                submission.selectedOptionId(),
                                                                question.getId())
                                                .orElseThrow(() -> new BadRequestException(
                                                                "Invalid option for question"));

                                correct = Boolean.TRUE.equals(
                                                selectedOption.getCorrect());
                        }

                        if (correct) {
                                pointsEarned = calculatePoints(question);
                        }

                        AttemptAnswer answer = AttemptAnswer.builder()
                                        .attempt(attempt)
                                        .question(question)
                                        .selectedOption(selectedOption)
                                        .correct(correct)
                                        .timeTakenMs(submission.timeTakenMs())
                                        .pointsEarned(pointsEarned)
                                        .build();

                        attemptAnswerRepository.save(answer);

                        if (correct) {
                                score += pointsEarned;
                                correctCount++;
                        }

                        Option correctOption = findCorrectOption(question.getId());

                        results.add(
                                        new AnswerResultDto(
                                                        question.getId(),
                                                        selectedOption != null
                                                                        ? selectedOption.getId()
                                                                        : null,
                                                        correct,
                                                        correctOption != null
                                                                        ? correctOption.getId()
                                                                        : null,
                                                        correctOption != null
                                                                        ? correctOption.getOptionText()
                                                                        : null,
                                                        pointsEarned,
                                                        submission.timeTakenMs()));
                }

                attempt.setCompletedAt(LocalDateTime.now());
                attempt.setScore(score);
                attempt.setCorrectCount(correctCount);

                attemptRepository.save(attempt);

                return new AttemptResultResponse(
                                attempt.getId(),
                                attempt.getScore(),
                                attempt.getCorrectCount(),
                                attempt.getTotalQuestions(),
                                attempt.getStartedAt(),
                                attempt.getCompletedAt(),
                                results);
        }

        public AttemptResultResponse getAttemptResult(UUID attemptId) {

                User user = getCurrentUser();

                Attempt attempt = attemptRepository
                                .findByIdAndUserId(attemptId, user.getId())
                                .orElseThrow(() -> new NotFoundException("Attempt not found"));

                if (attempt.getCompletedAt() == null) {
                        throw new BadRequestException(
                                        "Attempt is not completed yet");
                }

                List<AnswerResultDto> results = attemptAnswerRepository
                                .findByAttemptId(attemptId)
                                .stream()
                                .sorted(Comparator.comparing(
                                                AttemptAnswer::getId))
                                .map(this::toAnswerResultDto)
                                .toList();

                return new AttemptResultResponse(
                                attempt.getId(),
                                attempt.getScore(),
                                attempt.getCorrectCount(),
                                attempt.getTotalQuestions(),
                                attempt.getStartedAt(),
                                attempt.getCompletedAt(),
                                results);
        }

        private List<Question> selectRandomQuestions(Difficulty difficulty) {

                Pageable pageable = PageRequest.of(0, QUESTION_COUNT * 20);

                List<Question> candidates = questionRepository.findRandomActiveQuestions(pageable);

                List<Question> eligibleQuestions = candidates.stream()
                                .filter(question -> question.getDifficulty() == difficulty)
                                .toList();

                List<Question> mutableQuestions = new ArrayList<>(eligibleQuestions);

                Collections.shuffle(mutableQuestions);

                if (mutableQuestions.size() < QUESTION_COUNT) {
                        return mutableQuestions;
                }

                return mutableQuestions.subList(0, QUESTION_COUNT);
        }

        private int calculatePoints(Question question) {

                return switch (question.getDifficulty()) {
                        case EASY -> 1;
                        case MEDIUM -> 2;
                        case HARD -> 4;
                };
        }

        private User getCurrentUser() {
                return currentUserProvider.getCurrentUser();
        }

        private QuestionDto toQuestionDto(Question question) {

                List<OptionDto> options = question.getOptions()
                                .stream()
                                .sorted(Comparator.comparing(
                                                Option::getId))
                                .map(option -> new OptionDto(
                                                option.getId(),
                                                option.getOptionText()))
                                .toList();

                return new QuestionDto(
                                question.getId(),
                                question.getCategory() != null
                                                ? question.getCategory().getId()
                                                : null,
                                question.getPrompt(),
                                question.getDifficulty(),
                                options);
        }

        private AnswerResultDto toAnswerResultDto(
                        AttemptAnswer answer) {

                Option correctOption = findCorrectOption(
                                answer.getQuestion().getId());

                return new AnswerResultDto(
                                answer.getQuestion().getId(),
                                answer.getSelectedOption() != null
                                                ? answer.getSelectedOption().getId()
                                                : null,
                                Boolean.TRUE.equals(answer.getCorrect()),
                                correctOption != null
                                                ? correctOption.getId()
                                                : null,
                                correctOption != null
                                                ? correctOption.getOptionText()
                                                : null,
                                answer.getPointsEarned(),
                                answer.getTimeTakenMs());
        }

        private Option findCorrectOption(Long questionId) {

                return optionRepository
                                .findByQuestionId(questionId)
                                .stream()
                                .filter(option -> Boolean.TRUE.equals(
                                                option.getCorrect()))
                                .findFirst()
                                .orElse(null);
        }
}