package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class AccountDeletionTransactionTest extends DatabaseIntegrationTest {
    private static final String EMAIL = "user@example.com";

    @Autowired
    AccountWriteService accountDeletion;

    @Autowired
    ApplicationEvents events;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM UserCredential").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();
        });
    }

    @Test
    void 탈퇴하면_로그인_무효화_이벤트를_발행한다() {
        // given
        Long userId = saveUserWithPassword();

        // when
        accountDeletion.delete(userId, EMAIL, 0L);

        // then
        assertThat(events.stream(LoginsInvalidated.class))
                .containsExactly(new LoginsInvalidated(userId));
    }

    @Test
    void 인증_버전이_다르면_이벤트를_발행하지_않는다() {
        // given
        Long userId = saveUserWithPassword();

        // when
        assertThatThrownBy(() -> accountDeletion.delete(userId, EMAIL, 1L))
                .isInstanceOf(AuthException.class);

        // then
        assertThat(events.stream(LoginsInvalidated.class)).isEmpty();
    }

    private Long saveUserWithPassword() {
        return transactionTemplate.execute(status -> {
            User user = User.verifiedEmailMember(EMAIL, "회원", LocalDateTime.now(ZoneOffset.UTC));
            entityManager.persist(user);
            entityManager.persist(UserCredential.builder().user(user).passwordHash("hash").build());
            return user.getId();
        });
    }
}
