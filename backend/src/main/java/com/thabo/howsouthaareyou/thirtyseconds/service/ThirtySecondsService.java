package com.thabo.howsouthaareyou.thirtyseconds.service;

import com.thabo.howsouthaareyou.common.exception.BadRequestException;
import com.thabo.howsouthaareyou.thirtyseconds.dto.GameResultResponse;
import com.thabo.howsouthaareyou.thirtyseconds.dto.GameSummaryResponse;
import com.thabo.howsouthaareyou.thirtyseconds.dto.RoundDto;
import com.thabo.howsouthaareyou.thirtyseconds.dto.StartGameRequest;
import com.thabo.howsouthaareyou.thirtyseconds.dto.StartGameResponse;
import com.thabo.howsouthaareyou.thirtyseconds.dto.SubmitRoundScoreRequest;
import com.thabo.howsouthaareyou.thirtyseconds.dto.SubmitRoundScoreResponse;
import com.thabo.howsouthaareyou.thirtyseconds.entity.ThirtySecondsCard;
import com.thabo.howsouthaareyou.thirtyseconds.entity.ThirtySecondsMode;
import com.thabo.howsouthaareyou.thirtyseconds.repository.ThirtySecondsCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThirtySecondsService {

    private static final int ROUND_DURATION_SECONDS = 30;

    private final ThirtySecondsCardRepository cardRepository;

    public StartGameResponse startGame(StartGameRequest request) {
        List<String> playerNames = normalizePlayerNames(request);

        int roundsPerPlayer = request.roundsPerPlayer() == null ? 3 : request.roundsPerPlayer();
        int totalRounds = playerNames.size() * roundsPerPlayer;

        List<ThirtySecondsCard> cards = cardRepository.findRandomCards(PageRequest.of(0, totalRounds));

        if (cards.size() < totalRounds) {
            throw new BadRequestException("Not enough cards available in the database. Please seed more content.");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        UUID gameId = UUID.randomUUID();

        List<RoundDto> roundDtos = new ArrayList<>();

        for (int i = 0; i < totalRounds; i++) {
            String playerName = playerNames.get(i % playerNames.size());
            ThirtySecondsCard card = cards.get(i);

            List<String> shuffledWords = new ArrayList<>(card.getWords());
            Collections.shuffle(shuffledWords);
            String prompt = String.join(", ", shuffledWords);

            roundDtos.add(
                    new RoundDto(
                            (long) (i + 1),
                            i + 1,
                            playerName,
                            prompt,
                            null
                    ));
        }

        return new StartGameResponse(
                gameId,
                request.mode(),
                playerNames.size(),
                roundsPerPlayer,
                totalRounds,
                ROUND_DURATION_SECONDS,
                startedAt,
                roundDtos);
    }

    public SubmitRoundScoreResponse submitRoundScore(UUID gameId, SubmitRoundScoreRequest request) {
        return new SubmitRoundScoreResponse(
                request.roundId(),
                1,
                "Player",
                request.score(),
                false);
    }

    public GameResultResponse completeGame(UUID gameId) {
        return new GameResultResponse(
                gameId,
                ThirtySecondsMode.SOLO,
                1,
                0,
                "You",
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(),
                List.of());
    }

    public GameResultResponse getGameResult(UUID gameId) {
        return new GameResultResponse(
                gameId,
                ThirtySecondsMode.SOLO,
                1,
                0,
                "You",
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(),
                List.of());
    }

    public Page<GameSummaryResponse> getGameHistory(Pageable pageable) {
        return Page.empty(pageable);
    }

    private List<String> normalizePlayerNames(StartGameRequest request) {
        List<String> names = request.playerNames() == null
                ? List.of()
                : request.playerNames()
                        .stream()
                        .map(String::trim)
                        .filter(name -> !name.isBlank())
                        .toList();

        if (request.mode() == ThirtySecondsMode.SOLO) {
            return names.isEmpty() ? List.of("You") : List.of(names.get(0));
        }

        if (names.isEmpty()) {
            throw new BadRequestException("Player names are required for LOCAL mode");
        }

        if (names.size() > 8) {
            throw new BadRequestException("Maximum of 8 players allowed");
        }

        return names;
    }
}
