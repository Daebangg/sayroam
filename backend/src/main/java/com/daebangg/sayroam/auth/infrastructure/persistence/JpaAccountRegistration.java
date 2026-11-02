package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountRegistration;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Slf4j
@Repository
public class JpaAccountRegistration implements AccountRegistration {
    private static final HibernateJpaDialect JPA_DIALECT = new HibernateJpaDialect();
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public JpaAccountRegistration(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public long register(String email, String nickname, String passwordHash, Instant emailVerifiedAt) {
        try {
            // execute가 커밋까지 마친 뒤에만 반환한다. 두 INSERT와 커밋 오류 모두 아래에서 변환한다.
            return Objects.requireNonNull(transaction.execute(status -> {
                User user = User.verifiedEmailMember(email, nickname,
                        LocalDateTime.ofInstant(emailVerifiedAt, ZoneOffset.UTC));
                entityManager.persist(user);
                entityManager.persist(UserCredential.builder().user(user).passwordHash(passwordHash).build());
                entityManager.flush();
                return user.getId();
            }));
        } catch (RuntimeException exception) {
            if (isDuplicateEmail(exception)) {
                throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED, exception);
            }
            // 연결·잠금·커밋 장애만 일시적 실패로 변환하고 잘못된 매핑 등은 500으로 남긴다.
            DataAccessException translated = exception instanceof DataAccessException dataAccess
                    ? dataAccess : JPA_DIALECT.translateExceptionIfPossible(exception);
            if (translated instanceof DataAccessResourceFailureException
                    || translated instanceof TransientDataAccessException
                    || translated instanceof RecoverableDataAccessException
                    || exception instanceof CannotCreateTransactionException
                    || exception instanceof TransactionSystemException) {
                log.warn("Account registration persistence failed: {}", exception.getClass().getSimpleName());
                throw new AuthException(AuthErrorCode.SIGNUP_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }

    private static boolean isDuplicateEmail(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                // MySQL은 테이블명을 붙인 인덱스 이름을 반환할 수도 있다.
                if (constraint != null && (constraint.equals("uk_users_email")
                        || constraint.endsWith(".uk_users_email"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
