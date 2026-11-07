package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.in.AccountDeletionCleanup;
import com.daebangg.sayroam.auth.application.port.out.AccountDeletion;
import com.daebangg.sayroam.user.domain.entity.User;
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
public class JpaAccountDeletion implements AccountDeletion {
    private static final HibernateJpaDialect JPA_DIALECT = new HibernateJpaDialect();
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    private final AccountDeletionCleanup cleanup;

    public JpaAccountDeletion(EntityManager entityManager, PlatformTransactionManager transactionManager,
                              AccountDeletionCleanup cleanup) {
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.cleanup = cleanup;
    }

    @Override
    public void delete(long userId, String verifiedEmail, long expectedAuthVersion) {
        try {
            transaction.executeWithoutResult(status -> {
                // 비밀번호 변경과 같은 회원 잠금 순서를 사용해 두 민감 작업을 직렬화한다.
                User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
                if (user == null || !user.allowsAccountChange(verifiedEmail, expectedAuthVersion)) {
                    throw new AuthException(AuthErrorCode.ACCOUNT_DELETION_CONFLICT);
                }
                // 회원 행은 삭제하지 않는다. 다른 도메인은 기존 Long 회원 ID를 계속 참조한다.
                cleanup.deleteOwnedData(userId);
                user.withdraw(LocalDateTime.now(ZoneOffset.UTC));
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
                log.warn("Account deletion persistence failed: {}", exception.getClass().getSimpleName());
                throw new AuthException(AuthErrorCode.ACCOUNT_DELETION_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }
}
