package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialAccounts;
import com.daebangg.sayroam.user.domain.SocialProvider;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserSocialAccount;
import jakarta.persistence.EntityManager;
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
