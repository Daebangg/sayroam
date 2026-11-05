package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.PasswordChange;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Repository
public class JpaPasswordChange implements PasswordChange {
    private static final HibernateJpaDialect JPA_DIALECT = new HibernateJpaDialect();
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public JpaPasswordChange(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void change(long userId, String verifiedEmail, long expectedAuthVersion, String passwordHash) {
        try {
            transaction.executeWithoutResult(status -> {
                User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
                if (user == null || !user.allowsAccountChange(verifiedEmail, expectedAuthVersion)) {
                    throw new AuthException(AuthErrorCode.PASSWORD_CHANGE_CONFLICT);
                }
                // 같은 도메인의 자격 정보를 회원 → 자격 정보 순서로 잠근다.
                UserCredential credential = entityManager.createQuery("""
                        select c from UserCredential c where c.user.id = :id
                        """, UserCredential.class).setParameter("id", userId)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst()
                        .orElseThrow(() -> new AuthException(AuthErrorCode.PASSWORD_CHANGE_NOT_ALLOWED));
                credential.changePassword(passwordHash, LocalDateTime.now(ZoneOffset.UTC));
                user.invalidateLogins();
                entityManager.flush();
            });
        } catch (AuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            DataAccessException translated = exception instanceof DataAccessException dataAccess
                    ? dataAccess : JPA_DIALECT.translateExceptionIfPossible(exception);
            if (translated instanceof DataAccessResourceFailureException
                    || translated instanceof TransientDataAccessException
                    || translated instanceof RecoverableDataAccessException
                    || exception instanceof CannotCreateTransactionException
                    || exception instanceof TransactionSystemException) {
                log.warn("Password change persistence failed: {}", exception.getClass().getSimpleName());
                throw new AuthException(AuthErrorCode.PASSWORD_CHANGE_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }
}
