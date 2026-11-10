package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.global.mail.EmailLanguage;
import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {
    private final AccountLookup accounts = mock(AccountLookup.class);
    private final PasswordResetStore store = mock(PasswordResetStore.class);
    private final VerificationEmailSender mail = mock(VerificationEmailSender.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final PasswordChange changes = mock(PasswordChange.class);
    private final PasswordResetService service = new PasswordResetService(accounts, store, Optional.of(mail), passwords, changes);
    private static final String EMAIL = "member@example.test";
    private static final String PROOF = "p".repeat(43);
    private static final PasswordResetStore.Subject SUBJECT = new PasswordResetStore.Subject(55, EMAIL, 3);

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void rejectsInvalidPasswordBeforeAnySideEffects(String password) {
        assertThatThrownBy(() -> service.reset(EMAIL, PROOF, password))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PASSWORD_POLICY_VIOLATION));
        verifyNoInteractions(accounts, store, mail, passwords, changes);
    }

    @Test
    void hashesBeforeConsumingProofAndUsesItsCurrentAccountBinding() {
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.of(new AccountLookup.Credentials(55, "old-hash", 3)));
        when(passwords.hash("NewPass1!")).thenReturn("new-hash");
        when(store.consumeProof(eq(SUBJECT), anyString())).thenReturn(PasswordResetStore.ProofStatus.CONSUMED);
        service.reset(" MEMBER@EXAMPLE.TEST ", PROOF, "NewPass1!");
        InOrder order = inOrder(passwords, store, changes);
        order.verify(passwords).hash("NewPass1!");
        order.verify(store).consumeProof(eq(SUBJECT), anyString());
        order.verify(changes).change(55, EMAIL, 3, "new-hash");
    }

    @Test
    void hashFailureDoesNotConsumeTheProofOrMutateTheAccount() {
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.of(new AccountLookup.Credentials(55, "old-hash", 3)));
        when(passwords.hash(anyString())).thenThrow(new IllegalStateException("test hasher failure"));
        assertThatThrownBy(() -> service.reset(EMAIL, PROOF, "Replacement-pass1!")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(store, changes);
    }

    @Test
    void failedProofConsumptionDoesNotAttemptDatabaseMutation() {
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.of(new AccountLookup.Credentials(55, "old-hash", 3)));
        when(passwords.hash(anyString())).thenReturn("new-hash");
        when(store.consumeProof(eq(SUBJECT), anyString())).thenThrow(new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE));
        assertThatThrownBy(() -> service.reset(EMAIL, PROOF, "Replacement-pass1!"))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE));
        verifyNoInteractions(changes);
    }

    @Test
    void disabledMailDoesNotQueryAccountsOrConsumeRateLimits() {
        PasswordResetService disabled = new PasswordResetService(accounts, store, Optional.empty(), passwords, changes);
        assertThatThrownBy(() -> disabled.request(EMAIL, "127.0.0.1", EmailLanguage.KO))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_DELIVERY_DISABLED));
        verifyNoInteractions(accounts, store, mail);
    }

    @Test
    void cleanupFailurePreservesDeliveryCauseWithoutActivatingCode() {
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.empty());
        when(store.reserve(any(), anyString(), anyString(), anyString())).thenReturn(Duration.ZERO);
        AuthException delivery = new AuthException(AuthErrorCode.EMAIL_DELIVERY_FAILED);
        AuthException cleanup = new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE);
        doThrow(delivery).when(mail).sendPasswordResetCode(eq(EMAIL), anyString(), eq(EmailLanguage.KO));
        doThrow(cleanup).when(store).discard(any(), anyString());
        assertThatThrownBy(() -> service.request(EMAIL, "127.0.0.1", EmailLanguage.KO)).isSameAs(cleanup);
        assertThat(cleanup.getSuppressed()).containsExactly(delivery);
        verify(store, never()).activate(any(), anyString());
    }

    static Stream<String> invalidPasswords() { return Stream.of(null, "", "short", "        ", "x".repeat(129)); }
}
