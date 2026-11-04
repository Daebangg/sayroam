package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
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
public class JpaAccountLookup implements AccountLookup {
    private static final HibernateJpaDialect JPA_DIALECT = new HibernateJpaDialect();
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public JpaAccountLookup(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setReadOnly(true);
    }

    @Override
    public Optional<Credentials> findActiveCredentials(String normalizedEmail) {
        return read(() -> entityManager.createQuery("""
                select new com.daebangg.sayroam.auth.application.port.out.AccountLookup$Credentials(u.id, c.passwordHash)
                from UserCredential c join c.user u
                where u.email = :email and u.deletedAt is null and u.emailVerifiedAt is not null
                """, Credentials.class).setParameter("email", normalizedEmail).getResultList().stream().findFirst());
    }

    @Override
    public Optional<AuthenticatedAccount> findActiveAccount(long userId) {
        return read(() -> entityManager.createQuery("""
                select new com.daebangg.sayroam.auth.domain.AuthenticatedAccount(
                    u.id, u.email, u.nickname, u.profileImageUrl, u.role)
                from User u where u.id = :id and u.deletedAt is null
                """, AuthenticatedAccount.class).setParameter("id", userId).getResultList().stream().findFirst());
    }

    private <T> T read(Supplier<T> query) {
        try {
            return Objects.requireNonNull(transaction.execute(status -> query.get()));
        } catch (RuntimeException exception) {
            DataAccessException translated = exception instanceof DataAccessException dataAccess
                    ? dataAccess : JPA_DIALECT.translateExceptionIfPossible(exception);
            if (translated instanceof DataAccessResourceFailureException
                    || translated instanceof TransientDataAccessException
                    || translated instanceof RecoverableDataAccessException
                    || exception instanceof CannotCreateTransactionException
                    || exception instanceof TransactionSystemException) {
                throw new AuthException(AuthErrorCode.AUTHENTICATION_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }
}
