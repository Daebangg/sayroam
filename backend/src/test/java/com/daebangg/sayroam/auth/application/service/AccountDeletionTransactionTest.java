package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.User;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Autowired
    RoomService roomService;

    @AfterEach
    void cleanUp() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM RoomMember").executeUpdate();
            entityManager.createQuery("DELETE FROM Room").executeUpdate();
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

    @Test
    void 탈퇴하면_참여_중인_모든_방에서_나간다() {
        // given
        Long userId = saveUserWithPassword();
        Long firstRoomId = roomService.createForTrip();
        Long secondRoomId = roomService.createForTrip();
        roomService.join(firstRoomId, userId);
        roomService.join(secondRoomId, userId);

        // when
        accountDeletion.delete(userId, EMAIL, 0L);

        // then
        assertThat(roomService.countJoinedMembers(firstRoomId)).isZero();
        assertThat(roomService.countJoinedMembers(secondRoomId)).isZero();
    }

    @Test
    void 탈퇴가_거부되면_방에_그대로_남는다() {
        // given
        Long userId = saveUserWithPassword();
        Long roomId = roomService.createForTrip();
        roomService.join(roomId, userId);

        // when
        assertThatThrownBy(() -> accountDeletion.delete(userId, EMAIL, 1L))
                .isInstanceOf(AuthException.class);

        // then
        assertThatCode(() -> roomService.ensureJoined(roomId, userId)).doesNotThrowAnyException();
    }

    private Long saveUserWithPassword() {
        return transactionTemplate.execute(status -> {
            User user = User.verifiedEmailMember(EMAIL, "회원", LocalDateTime.now(ZoneOffset.UTC), "hash");
            entityManager.persist(user);
            return user.getId();
        });
    }
}
