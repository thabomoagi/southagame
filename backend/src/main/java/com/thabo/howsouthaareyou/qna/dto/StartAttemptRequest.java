package com.thabo.howsouthaareyou.qna.dto;

import com.thabo.howsouthaareyou.qna.entity.Difficulty;
import jakarta.validation.constraints.NotNull;

public record StartAttemptRequest(
        @NotNull Difficulty difficulty) {
}