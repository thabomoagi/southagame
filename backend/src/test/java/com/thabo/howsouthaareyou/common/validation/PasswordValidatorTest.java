package com.thabo.howsouthaareyou.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    @Test
    void rejects_shortPasswords() {
        assertThat(PasswordValidator.isValid("A1")).isFalse();
        assertThat(PasswordValidator.isValid("abcdefg")).isFalse();
    }

    @Test
    void rejects_passwordsMissingLetterOrNumber() {
        assertThat(PasswordValidator.isValid("12345678")).isFalse();
        assertThat(PasswordValidator.isValid("abcdefgh")).isFalse();
    }

    @Test
    void accepts_longPasswordsWithLetterAndNumber() {
        assertThat(PasswordValidator.isValid("southa1way")).isTrue();
        assertThat(PasswordValidator.isValid("P@ssw0rd1")).isTrue();
    }

    @Test
    void rejects_null() {
        assertThat(PasswordValidator.isValid(null)).isFalse();
    }
}