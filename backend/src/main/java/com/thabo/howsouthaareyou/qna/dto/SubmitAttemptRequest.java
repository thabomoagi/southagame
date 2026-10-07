package com.thabo.howsouthaareyou.qna.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAttemptRequest(
        @NotNull @Valid List<AnswerSubmission> answers) {
}