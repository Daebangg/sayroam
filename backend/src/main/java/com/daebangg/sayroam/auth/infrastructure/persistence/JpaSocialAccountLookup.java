package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.port.out.SocialAccountLookup;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.SocialProvider;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class JpaSocialAccountLookup implements SocialAccountLookup {
    private static final HibernateJpaDialect DIALECT = new HibernateJpaDialect();
    private final EntityManager entities;
    private final TransactionTemplate read;

    public JpaSocialAccountLookup(EntityManager entities, PlatformTransactionManager manager) {
        this.entities = entities;
        this.read = new TransactionTemplate(manager);
        this.read.setReadOnly(true);
    }

    @Override
    public Optional<SocialAccountResult> findActive(SocialProvider provider, String providerUserId) {
        return execute(() -> read.execute(status -> entities.createQuery("""
                select new com.daebangg.sayroam.auth.application.dto.SocialAccountResult(u.id, u.authVersion)
                from UserSocialAccount s join s.user u
                where s.provider = :provider and s.providerUserId = :subject
                    and u.deletedAt is null and u.emailVerifiedAt is not null
                """, SocialAccountResult.class).setParameter("provider", provider).setParameter("subject", providerUserId)
                .getResultList().stream().findFirst()));
    }

    @Override
    public boolean emailExists(String email) {
        return execute(() -> read.execute(status -> entities.createQuery(
                "select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", email).getSingleResult() > 0));
    }

    @Override
    public Optional<SocialAccountResult> findActiveByEmail(String email) {
        return execute(() -> read.execute(status -> entities.createQuery("""
                select new com.daebangg.sayroam.auth.application.dto.SocialAccountResult(u.id, u.authVersion)
                from User u where u.email = :email and u.deletedAt is null and u.emailVerifiedAt is not null
                """, SocialAccountResult.class).setParameter("email", email).getResultList().stream().findFirst()));
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return Objects.requireNonNull(action.get());
        } catch (RuntimeException exception) {
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
