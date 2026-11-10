package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountRegistration;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SignupServiceTests {
    private final EmailVerificationStore verification = mock(EmailVerificationStore.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final AccountRegistration accounts = mock(AccountRegistration.class);
    private final SignupService service = new SignupService(verification, passwords, accounts);

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void rejectsInvalidPasswordWithoutConsumingProofHashingOrRegistering(String password) {
        assertThatThrownBy(() -> service.signup("member@example.test", password, "member", "unused-proof"))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_POLICY_VIOLATION));
        verifyNoInteractions(verification, passwords, accounts);
    }

    @Test
    void preservesPasswordSpacesAndCaseWhenHashingAValidSignup() {
        String email = "member@example.test";
        String password = "SignUpPass1!";
        String proof = "p".repeat(43);
        Instant verifiedAt = Instant.parse("2026-01-01T00:00:00Z");
        when(verification.consumeSignupProof(email, VerificationDigests.sha256(proof)))
                .thenReturn(new EmailVerificationStore.ProofConsumption(EmailVerificationStore.ProofStatus.CONSUMED, verifiedAt));
        when(passwords.hash(password)).thenReturn("encoded-password");
        when(accounts.register(email, "member", "encoded-password", verifiedAt)).thenReturn(42L);

        assertThat(service.signup(" Member@Example.test ", password, "member", proof)).isEqualTo(42L);
        verify(passwords).hash(password);
        verify(accounts).register(email, "member", "encoded-password", verifiedAt);
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(null, "", "short", "        ", "x".repeat(129));
    }
}
