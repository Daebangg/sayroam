package com.daebangg.sayroam.auth.domain;

import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.presentation.dto.*;
import com.daebangg.sayroam.user.domain.Nickname;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputPolicyTests {
    @Test
    void supportsPracticalAsciiEmailLengthsAndRejectsInvalidLabelsAndLocalParts() {
        String maximum = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertThat(maximum.length()).isEqualTo(254);
        for (String value : List.of(maximum, " First.Last+tag@Example.COM ", "a@xn--example-9d0b.test")) {
            assertThat(EmailAddress.isValid(value)).as(value).isTrue();
        }
        for (String value : List.of("a".repeat(65) + "@example.test", "a@" + "b".repeat(64) + ".test",
                "a..b@example.test", ".a@example.test", "a.@example.test", "a@-example.test", "a@exam_ple.test", "a@localhost", "한글@example.test", "a@예시.test")) {
            assertThat(EmailAddress.isValid(value)).as(value).isFalse();
        }
        assertThat(EmailAddress.normalize("\u00a0USER@EXAMPLE.TEST\uFEFF")).isEqualTo("user@example.test");
    }

    @Test
    void signupChangeAndResetApplyIdenticalNewPasswordRequirements() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            String proof = "a".repeat(43);
            for (String password : List.of("Aa1!aaaa", "Aa1!" + "a".repeat(20), "lowercase1!", "Uppercase!",
                    "Uppercase1", "Aa1! aaab", "Aa1!\u200baaa", "Aa1!" + "a".repeat(21))) {
                boolean valid = acceptsPassword(password);
                assertThat(validator.validate(new SignupRequest("member@example.test", password, "여행자", proof)).isEmpty()).isEqualTo(valid);
                assertThat(validator.validate(new PasswordChangeRequest(proof, password)).isEmpty()).isEqualTo(valid);
                assertThat(validator.validate(new PasswordResetRequest("member@example.test", proof, password)).isEmpty()).isEqualTo(valid);
            }
            assertThat(validator.validate(new LoginRequest("member@example.test", "x".repeat(128), false))).isEmpty();
        }
    }

    private boolean acceptsPassword(String password) {
        try {
            new Password(password);
            return true;
        } catch (AuthException exception) {
            return false;
        }
    }

    @Test
    void nicknamesKeepVisibleUnicodeAndInternalSpacesWithin50Characters() {
        assertThat(new Nickname("\u00a0제주 여행자😀\uFEFF").value()).isEqualTo("제주 여행자😀");
        assertThat(new Nickname("가".repeat(50)).value()).hasSize(50);
        for (String nickname : List.of("가".repeat(51), "name\u0085", "name\u200b", "name\u2028next", "name\u0000")) {
            assertThatThrownBy(() -> new Nickname(nickname)).isInstanceOf(RuntimeException.class);
        }
    }
}
