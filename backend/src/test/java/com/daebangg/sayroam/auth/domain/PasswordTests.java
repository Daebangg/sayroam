package com.daebangg.sayroam.auth.domain;

import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Aa1!aaa", "        ", "\t\n\r        ", "lowercase1!", "Uppercase!", "Uppercase1", "Aa1! aaab", "Aa1!\u200Baaa", "Aa1!\u0085aaa"})
    void rejectsMissingShortAndWhitespaceOnlyPasswords(String password) {
        assertInvalid(password);
    }

    @Test
    void rejectsPasswordsLongerThan24Characters() {
        assertInvalid("Aa1!" + "a".repeat(21));
    }

    @ParameterizedTest
    @MethodSource("validPasswords")
    void acceptsBoundaryLengthsAndVisibleUnicodeWithAllRequirements(String password) {
        assertThat(new Password(password).value()).isEqualTo(password);
    }

    static Stream<String> validPasswords() {
        return Stream.of("Aa1!aaaa", "Aa1!" + "a".repeat(20), "Aa1!가나다라", "Aa1😀aaaa");
    }

    private void assertInvalid(String password) {
        assertThatThrownBy(() -> new Password(password))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_POLICY_VIOLATION));
    }
}
