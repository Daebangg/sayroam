package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAuthenticationService {
    private final AccountLookup accounts;

    public void ensureActive(long userId, long authVersion) {
        AuthenticatedAccount account = accounts.findActiveAccount(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED));
        if (account.authVersion() != authVersion) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
        }
    }
}
