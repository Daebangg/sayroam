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
    @ValueSource(strings = {"Aa1!aaa", "        ", "\t\n\r        ", "lowercase1!", "Uppercase!", "Uppercase1", "Aa1! aaab", "Aa1!\u200Baaa", "Aa1!\u0085aaa"})
    void rejectsMissingShortAndWhitespaceOnlyPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password)).isFalse();
    }

    @Test
    void rejectsPasswordsLongerThan24Characters() {
        assertThat(PasswordPolicy.isValid("Aa1!" + "a".repeat(21))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("validPasswords")
    void acceptsBoundaryLengthsAndVisibleUnicodeWithAllRequirements(String password) {
        assertThat(PasswordPolicy.isValid(password)).isTrue();
    }

    static Stream<String> validPasswords() {
        return Stream.of("Aa1!aaaa", "Aa1!" + "a".repeat(20), "Aa1!가나다라", "Aa1😀aaaa");
    }
}
