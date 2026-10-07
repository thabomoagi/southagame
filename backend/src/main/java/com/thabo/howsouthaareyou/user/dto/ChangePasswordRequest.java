package com.thabo.howsouthaareyou.user.dto;

import com.thabo.howsouthaareyou.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @ValidPassword String newPassword) {
}