package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import com.daebangg.sayroam.user.domain.UserRepository;
import com.daebangg.sayroam.user.domain.UserSocialAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
public class AccountWriteService {
    private final UserRepository users;
    private final AccountDeletionCleanupService cleanup;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;

    public AccountWriteService(UserRepository users, AccountDeletionCleanupService cleanup,
                               ApplicationEventPublisher events, PlatformTransactionManager manager) {
        this.users = users;
        this.cleanup = cleanup;
        this.events = events;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public long register(String email, String nickname, String passwordHash, Instant emailVerifiedAt) {
        return write(AuthErrorCode.SIGNUP_UNAVAILABLE, () -> {
            User user = User.verifiedEmailMember(email, nickname, LocalDateTime.ofInstant(emailVerifiedAt, ZoneOffset.UTC));
            users.addUser(user);
            users.addCredential(UserCredential.builder().user(user).passwordHash(passwordHash).build());
            users.flush();
            return user.getId();
        });
    }

    public SocialAccountResult registerSocial(SocialIdentity identity, String email, String nickname, Instant verifiedAt) {
        return write(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, () -> {
            User user = User.verifiedSocialMember(email, nickname, identity.profileImageUrl(),
                    LocalDateTime.ofInstant(verifiedAt, ZoneOffset.UTC));
            users.addUser(user);
            addSocialAccount(user, identity);
            return new SocialAccountResult(user.getId(), user.getAuthVersion());
        });
    }

    public SocialAccountResult linkVerifiedEmail(SocialAccountResult account, SocialIdentity identity) {
        return write(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, () -> {
            User user = lockActiveUser(account.userId(), AuthErrorCode.AUTHENTICATION_REVOKED);
            if (!user.allowsAccountChange(user.getEmail(), account.authVersion())) {
                throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
            }
            if (!identity.emailVerified() || !user.getEmail().equals(EmailAddress.normalize(identity.email()))) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
            }
            // 회원별 공급자 중복은 회원 잠금으로 직렬화하고, 다른 회원의 동일 소셜 ID는 DB에서 막는다.
            Optional<String> subject = users.findSocialSubject(account.userId(), identity.provider());
            if (subject.isPresent()) {
                if (subject.orElseThrow().equals(identity.providerUserId())) {
                    return account;
                }
                throw new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_CONFLICT);
            }
            addSocialAccount(user, identity);
            return account;
        });
    }

    private void addSocialAccount(User user, SocialIdentity identity) {
        users.addSocialAccount(UserSocialAccount.builder().user(user).provider(identity.provider())
                .providerUserId(identity.providerUserId()).email(identity.email()).build());
        users.flush();
    }

    public void change(long userId, String verifiedEmail, long expectedAuthVersion, String passwordHash) {
        write(AuthErrorCode.PASSWORD_CHANGE_UNAVAILABLE, () -> {
            User user = lockVerifiedUser(userId, verifiedEmail, expectedAuthVersion, AuthErrorCode.PASSWORD_CHANGE_CONFLICT);
            // 회원 행을 먼저 잠근 뒤 같은 트랜잭션에서 자격 정보를 변경한다.
            UserCredential credential = users.findCredentialForUpdate(userId)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.PASSWORD_CHANGE_NOT_ALLOWED));
            credential.changePassword(passwordHash, LocalDateTime.now(ZoneOffset.UTC));
            user.invalidateLogins();
            publishInvalidation(userId);
            return null;
        });
    }

    public void delete(long userId, String verifiedEmail, long expectedAuthVersion) {
        write(AuthErrorCode.ACCOUNT_DELETION_UNAVAILABLE, () -> {
            User user = lockVerifiedUser(userId, verifiedEmail, expectedAuthVersion, AuthErrorCode.ACCOUNT_DELETION_CONFLICT);
            cleanup.deleteOwnedData(userId);
            user.withdraw(LocalDateTime.now(ZoneOffset.UTC));
            publishInvalidation(userId);
            return null;
        });
    }

    private User lockVerifiedUser(long userId, String verifiedEmail, long expectedAuthVersion, AuthErrorCode conflict) {
        User user = lockActiveUser(userId, conflict);
        if (!user.allowsAccountChange(verifiedEmail, expectedAuthVersion)) {
            throw new AuthException(conflict);
        }
        return user;
    }

    private User lockActiveUser(long userId, AuthErrorCode conflict) {
        return users.findForUpdateByIdAndDeletedAtIsNull(userId).orElseThrow(() -> new AuthException(conflict));
    }

    private void publishInvalidation(long userId) {
        users.flush();
        // 트랜잭션 안에서 발행해야 커밋 후 리스너가 받는다.
        events.publishEvent(new LoginsInvalidated(userId));
    }

    private <T> T write(AuthErrorCode unavailable, Supplier<T> action) {
        try {
            // 증명 소비·해시·토큰 발급과 DB 쓰기 범위를 나누고 커밋 오류까지 변환한다.
            return transaction.execute(status -> action.get());
        } catch (UserRepository.DuplicateEmail exception) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED, exception);
        } catch (UserRepository.DuplicateSocialAccount exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_CONFLICT, exception);
        } catch (DataAccessResourceFailureException | TransientDataAccessException | RecoverableDataAccessException
                 | CannotCreateTransactionException | TransactionSystemException exception) {
            log.warn("Account write failed with {}: {}", unavailable, exception.getClass().getSimpleName());
            throw new AuthException(unavailable, exception);
        }
    }
}
