package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.PasswordChange;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordChangeServiceTests {
    private final AccountLookup accounts = mock(AccountLookup.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final AccountVerificationService verification = mock(AccountVerificationService.class);
    private final PasswordChange changes = mock(PasswordChange.class);
    private final PasswordChangeService service = new PasswordChangeService(accounts, passwords, verification, changes);

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void rejectsInvalidPasswordBeforeLookupProofConsumptionOrMutation(String password) {
        assertThatThrownBy(() -> service.change(55, 3, "unused-proof", password))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_POLICY_VIOLATION));
        verifyNoInteractions(accounts, passwords, verification, changes);
    }

    @Test
    void preservesPasswordAndUsesTheVerifiedAccountBinding() {
        String email = "member@example.test";
        String password = "  New Pass!  ";
        String proof = "p".repeat(43);
        when(accounts.findActiveVerifiedAccount(55)).thenReturn(Optional.of(new AccountLookup.VerifiedAccount(email, 3)));
        when(accounts.findActiveCredentials(email)).thenReturn(Optional.of(new AccountLookup.Credentials(55, "old-hash", 3)));
        when(passwords.hash(password)).thenReturn("new-hash");
        when(verification.consumeProof(55, 3, AccountVerificationPurpose.PASSWORD_CHANGE, proof))
                .thenReturn(new AccountVerificationService.VerifiedAction(email, 3, Instant.parse("2026-01-01T00:00:00Z")));

        service.change(55, 3, proof, password);

        verify(passwords).hash(password);
        verify(changes).change(55, email, 3, "new-hash");
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(null, "", "short", "        ", "x".repeat(129));
    }
}
