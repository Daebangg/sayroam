package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import java.util.Optional;
import java.util.function.Supplier;

@Repository
@RequiredArgsConstructor
public class JpaAccountLookup implements AccountLookup {
    private final AccountLookupJpaRepository queries;

    @Override
    public boolean emailExists(String normalizedEmail) {
        // 기존 이메일 동등 비교는 NULL을 찾지 않는다. 파생 쿼리는 NULL 입력을 IS NULL로 바꾼다.
        return normalizedEmail != null
                && read(() -> queries.existsByEmail(normalizedEmail), AuthErrorCode.AUTHENTICATION_UNAVAILABLE);
    }

    @Override
    public Optional<Credentials> findActiveCredentials(String normalizedEmail) {
        return read(() -> queries.findActiveCredentials(normalizedEmail), AuthErrorCode.AUTHENTICATION_UNAVAILABLE);
    }

    @Override
    public Optional<AuthenticatedAccount> findActiveAccount(long userId) {
        return read(() -> queries.findActiveAccount(userId), AuthErrorCode.AUTHENTICATION_UNAVAILABLE);
    }

    @Override
    public Optional<VerifiedAccount> findActiveVerifiedAccount(long userId) {
        return read(() -> queries.findActiveVerifiedAccount(userId), AuthErrorCode.AUTHENTICATION_UNAVAILABLE);
    }

    @Override
    public Optional<SocialAccountResult> findActiveSocialAccount(SocialProvider provider, String providerUserId) {
        return read(() -> queries.findActiveSocialAccount(provider, providerUserId), AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
    }

    @Override
    public Optional<SocialAccountResult> findActiveSocialAccountByEmail(String normalizedEmail) {
        return read(() -> queries.findActiveSocialAccountByEmail(normalizedEmail), AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
    }

    private <T> T read(Supplier<T> query, AuthErrorCode unavailable) {
        try {
            return query.get();
        } catch (DataAccessResourceFailureException | TransientDataAccessException | RecoverableDataAccessException
                 | CannotCreateTransactionException | TransactionSystemException exception) {
            throw new AuthException(unavailable, exception);
        }
    }
}
