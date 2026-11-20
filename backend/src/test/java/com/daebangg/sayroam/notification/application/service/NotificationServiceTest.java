package com.daebangg.sayroam.notification.application.service;

import com.daebangg.sayroam.notification.application.dto.NotificationListQuery;
import com.daebangg.sayroam.notification.application.dto.NotificationResult;
import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class NotificationServiceTest extends DatabaseIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired
    NotificationService notificationService;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void 본인의_삭제되지_않은_알림만_최신순으로_조회하고_읽음_여부를_전달한다() {
        Long receiverId = createUser("수신자");
        Long otherId = createUser("다른 회원");
        Notification unread = saveNotification(receiverId, "trip-invite:1");
        Notification read = saveNotification(receiverId, "trip-invite:2");
        read.read(NOW);
        Notification deleted = saveNotification(receiverId, "trip-invite:3");
        deleted.delete(NOW);
        saveNotification(otherId, "trip-invite:4");
        flushAndClear();

        List<NotificationResult> results = notificationService.findAll(
                new NotificationListQuery(receiverId, null, 20));

        assertThat(results).extracting(NotificationResult::id)
                .containsExactly(read.getId(), unread.getId());
        assertThat(results).extracting(NotificationResult::isRead).containsExactly(true, false);
        assertThat(results.getFirst()).satisfies(result -> {
            assertThat(result.type()).isEqualTo(NotificationType.TRIP_INVITE);
            assertThat(result.content()).isEqualTo("여행에 초대되었습니다.");
            assertThat(result.targetType()).isEqualTo(NotificationTargetType.TRIP);
            assertThat(result.targetId()).isEqualTo(42L);
            assertThat(result.createdAt()).isNotNull();
        });
    }

    @Test
    void 다음_페이지는_기준_ID를_제외하고_이전_알림만_요청한_개수만큼_반환한다() {
        Long receiverId = createUser("수신자");
        Notification first = saveNotification(receiverId, "trip-invite:1");
        Notification second = saveNotification(receiverId, "trip-invite:2");
        Notification third = saveNotification(receiverId, "trip-invite:3");
        flushAndClear();

        List<NotificationResult> firstPage = notificationService.findAll(
                new NotificationListQuery(receiverId, null, 2));
        assertThat(firstPage).extracting(NotificationResult::id)
                .containsExactly(third.getId(), second.getId());

        saveNotification(receiverId, "trip-invite:new");
        flushAndClear();

        List<NotificationResult> nextPage = notificationService.findAll(
                new NotificationListQuery(receiverId, firstPage.getLast().id(), 2));
        assertThat(nextPage).extracting(NotificationResult::id).containsExactly(first.getId());
    }

    @Test
    void 연결_대상이_없는_알림도_조회할_수_있다() {
        Long receiverId = createUser("수신자");
        Notification notification = Notification.create(receiverId, NotificationType.TRIP_INVITE,
                null, null, "trip-invite:no-target", "안내 알림입니다.");
        entityManager.persist(notification);
        flushAndClear();

        assertThat(notificationService.findAll(new NotificationListQuery(receiverId, null, 20)))
                .singleElement().satisfies(result -> {
                    assertThat(result.targetType()).isNull();
                    assertThat(result.targetId()).isNull();
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void 조회_개수의_허용_경계값을_받고_알림이_없으면_빈_목록을_반환한다(int size) {
        Long receiverId = createUser("수신자");

        assertThat(notificationService.findAll(new NotificationListQuery(receiverId, null, size)))
                .isEmpty();
    }

    private Long createUser(String nickname) {
        User user = User.builder().nickname(nickname).role(Role.USER).build();
        entityManager.persist(user);
        return user.getId();
    }

    private Notification saveNotification(Long receiverId, String eventKey) {
        Notification notification = Notification.create(receiverId, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, 42L, eventKey, "여행에 초대되었습니다.");
        entityManager.persist(notification);
        return notification;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
