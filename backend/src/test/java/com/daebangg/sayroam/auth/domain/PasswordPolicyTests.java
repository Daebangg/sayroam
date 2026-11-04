package com.daebangg.sayroam.auth.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "        ", "\t\n\r        "})
    void rejectsMissingShortAndWhitespaceOnlyPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password)).isFalse();
    }

    @Test
    void rejectsPasswordsLongerThan128Characters() {
        assertThat(PasswordPolicy.isValid("x".repeat(129))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("validPasswords")
    void acceptsBoundaryLengthsUnicodeAndSpacesAroundNonblankContent(String password) {
        assertThat(PasswordPolicy.isValid(password)).isTrue();
    }

    static Stream<String> validPasswords() {
        return Stream.of("12345678", "x".repeat(128), "  Pass!  ", "가나다라마바사아");
    }
}
