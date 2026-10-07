package com.thabo.howsouthaareyou.auth.dto;

import com.thabo.howsouthaareyou.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

        @NotBlank
        private String token;

        @NotBlank
        @ValidPassword
        private String newPassword;
}