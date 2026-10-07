package com.thabo.howsouthaareyou.user.dto;

public record UserStatsResponse(
                long totalGamesPlayed,
                long totalMcqGames,
                long totalThirtySecondsGames,
                Integer dailyRank,
                Integer dailyScore,
                Integer weeklyRank,
                Integer weeklyScore,
                Integer monthlyRank,
                Integer monthlyScore,
                Integer allTimeRank,
                Integer allTimeScore) {
}