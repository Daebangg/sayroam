package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialAccounts;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.entity.UserSocialAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class JpaSocialAccounts implements SocialAccounts {
    private static final HibernateJpaDialect DIALECT = new HibernateJpaDialect();
    private final EntityManager entities;
    private final TransactionTemplate read;
    private final TransactionTemplate write;

    public JpaSocialAccounts(EntityManager entities, PlatformTransactionManager manager) {
        this.entities = entities;
        this.read = new TransactionTemplate(manager);
        this.read.setReadOnly(true);
        this.write = new TransactionTemplate(manager);
        this.write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Optional<Account> findActive(SocialProvider provider, String providerUserId) {
        return execute(() -> read.execute(status -> entities.createQuery("""
                select new com.daebangg.sayroam.auth.application.port.out.SocialAccounts$Account(u.id, u.authVersion)
                from UserSocialAccount s join s.user u
                where s.provider = :provider and s.providerUserId = :subject
                    and u.deletedAt is null and u.emailVerifiedAt is not null
                """, Account.class).setParameter("provider", provider).setParameter("subject", providerUserId)
                .getResultList().stream().findFirst()));
    }

    @Override
    public boolean emailExists(String email) {
        return execute(() -> read.execute(status -> entities.createQuery(
                "select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", email).getSingleResult() > 0));
    }

    @Override
    public Optional<Account> findActiveByEmail(String email) {
        return execute(() -> read.execute(status -> entities.createQuery("""
                select new com.daebangg.sayroam.auth.application.port.out.SocialAccounts$Account(u.id, u.authVersion)
                from User u where u.email = :email and u.deletedAt is null and u.emailVerifiedAt is not null
                """, Account.class).setParameter("email", email).getResultList().stream().findFirst()));
    }

    @Override
    public Account register(SocialIdentity identity, String email, String nickname, Instant verifiedAt) {
        return execute(() -> write.execute(status -> {
            User user = User.verifiedSocialMember(email, nickname, identity.profileImageUrl(), LocalDateTime.ofInstant(verifiedAt, ZoneOffset.UTC));
            entities.persist(user);
            entities.persist(UserSocialAccount.builder().user(user).provider(identity.provider())
                    .providerUserId(identity.providerUserId()).email(identity.email()).build());
            entities.flush();
            return new Account(user.getId(), 0);
        }));
    }

    @Override
    public Account linkVerifiedEmail(Account account, SocialIdentity identity) {
        return execute(() -> write.execute(status -> {
            User user = requireAccount(account, LockModeType.PESSIMISTIC_WRITE);
            if (!identity.emailVerified() || !user.getEmail().equals(EmailAddress.normalize(identity.email()))) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
            }
            // 회원별 공급자 중복은 회원 잠금으로 직렬화한다. 다른 회원의 동일 소셜 ID는 DB UNIQUE로 막는다.
            List<String> subjects = entities.createQuery("select s.providerUserId from UserSocialAccount s where s.user.id = :id and s.provider = :provider", String.class)
                    .setParameter("id", account.userId()).setParameter("provider", identity.provider()).getResultList();
            if (!subjects.isEmpty()) {
                if (subjects.getFirst().equals(identity.providerUserId())) return account;
                throw new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_CONFLICT);
            }
            entities.persist(UserSocialAccount.builder().user(user).provider(identity.provider())
                    .providerUserId(identity.providerUserId()).email(identity.email()).build());
            entities.flush();
            return account;
        }));
    }

    private User requireAccount(Account account, LockModeType lock) {
        User user = lock == null ? entities.find(User.class, account.userId())
                : entities.find(User.class, account.userId(), lock);
        if (user == null || !user.allowsAccountChange(user.getEmail(), account.authVersion())) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
        }
        return user;
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return Objects.requireNonNull(action.get());
        } catch (RuntimeException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                    String constraint = violation.getConstraintName();
                    if (constraint.equals("uk_users_email") || constraint.endsWith(".uk_users_email")) {
                        throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED, exception);
                    }
                    if (constraint.contains("uk_user_social_account_")) {
                        throw new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_CONFLICT, exception);
                    }
                }
            }
            DataAccessException translated = exception instanceof DataAccessException data ? data : DIALECT.translateExceptionIfPossible(exception);
            if (translated instanceof DataAccessResourceFailureException || translated instanceof TransientDataAccessException
                    || translated instanceof RecoverableDataAccessException || exception instanceof CannotCreateTransactionException
                    || exception instanceof TransactionSystemException) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }
}
