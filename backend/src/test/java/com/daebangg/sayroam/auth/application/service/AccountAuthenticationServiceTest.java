package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AccountAuthenticationServiceTest {
    private static final long USER_ID = 7L;
    private static final long AUTH_VERSION = 3L;

    private final AccountLookup accounts = mock(AccountLookup.class);
    private final AccountAuthenticationService service = new AccountAuthenticationService(accounts);

    @Test
    void 활성_회원의_인증_버전이_같으면_통과한다() {
        // given
        AuthenticatedAccount active = account(AUTH_VERSION);
        given(accounts.findActiveAccount(USER_ID)).willReturn(Optional.of(active));

        // when & then
        assertThat(service.ensureActive(USER_ID, AUTH_VERSION)).isSameAs(active);
    }

    @Test
    void 인증_버전이_다르면_거부한다() {
        // given
        given(accounts.findActiveAccount(USER_ID)).willReturn(Optional.of(account(AUTH_VERSION + 1)));

        // when & then
        assertThatThrownBy(() -> service.ensureActive(USER_ID, AUTH_VERSION))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_REVOKED));
    }

    @Test
    void 활성_회원이_없으면_거부한다() {
        // given
        given(accounts.findActiveAccount(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.ensureActive(USER_ID, AUTH_VERSION))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_REVOKED));
    }

    private AuthenticatedAccount account(long authVersion) {
        return new AuthenticatedAccount(USER_ID, "user@example.com", "회원", null, Role.USER, authVersion);
    }
}
