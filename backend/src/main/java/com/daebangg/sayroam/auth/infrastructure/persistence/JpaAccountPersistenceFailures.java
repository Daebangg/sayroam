package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.port.out.AccountPersistenceFailures;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

@Component
public class JpaAccountPersistenceFailures implements AccountPersistenceFailures {
    private final HibernateJpaDialect dialect = new HibernateJpaDialect();

    @Override
    public boolean isUnavailable(RuntimeException failure) {
        if (failure instanceof CannotCreateTransactionException || failure instanceof TransactionSystemException) {
            return true;
        }
        // 엔티티의 지연 조회는 리포지토리 프록시 밖에서 실행될 수 있다.
        DataAccessException translated = failure instanceof DataAccessException dataAccess
                ? dataAccess : dialect.translateExceptionIfPossible(failure);
        return translated instanceof DataAccessResourceFailureException
                || translated instanceof TransientDataAccessException
                || translated instanceof RecoverableDataAccessException;
    }
}
