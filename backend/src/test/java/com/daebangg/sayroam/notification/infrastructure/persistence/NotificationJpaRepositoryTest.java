package com.daebangg.sayroam.notification.infrastructure.persistence;

import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationRepository;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class NotificationJpaRepositoryTest extends DatabaseIntegrationTest {
    private static final String EVENT_KEY = "matching-found:42";
    private static final String CONTENT = "조건에 맞는 동행 후보를 발견했어요.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Autowired
    NotificationJpaRepository notifications;

    @Autowired
    JdbcTemplate jdbc;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void 저장한_알림을_수신자와_ID로_다시_조회한다() {
        Long receiverId = createUser("수신자");
        Long id = saveNotification(receiverId);

        Notification found = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();

        assertThat(found).extracting("receiverId", "type", "targetType", "targetId", "eventKey", "content")
                .containsExactly(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                        NotificationTargetType.COMPANION_SEARCH, 42L, EVENT_KEY, CONTENT);
        assertThat(found.isRead()).isFalse();
        assertThat(found.isDeleted()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void 다른_사용자는_알림_ID를_알아도_조회할_수_없다() {
        Long receiverId = createUser("수신자");
        Long otherId = createUser("다른 회원");
        Long id = saveNotification(receiverId);

        Optional<Notification> found = notifications.findByIdAndReceiverId(id, otherId);

        assertThat(found).isEmpty();
    }

    @Test
    void 존재하지_않는_알림은_빈_결과를_반환한다() {
        Long receiverId = createUser("수신자");

        assertThat(notifications.findByIdAndReceiverId(Long.MAX_VALUE, receiverId)).isEmpty();
    }

    @Test
    void 조회한_알림의_읽음과_미읽음_변경은_다시_저장하지_않아도_반영된다() {
        Long receiverId = createUser("수신자");
        Long id = saveNotification(receiverId);
        Notification notification = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();

        notification.read(NOW);
        flushAndClear();

        Notification read = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        assertThat(read.isRead()).isTrue();
        assertThat(read).hasFieldOrPropertyWithValue("readAt", NOW);

        read.read(NOW.plusHours(1));
        flushAndClear();

        Notification reread = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        assertThat(reread).hasFieldOrPropertyWithValue("readAt", NOW);
        reread.unread();
        flushAndClear();

        Notification unread = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        assertThat(unread.isRead()).isFalse();
        assertThat(unread).hasFieldOrPropertyWithValue("readAt", null);
    }

    @Test
    void 삭제한_알림도_본인_조회가_가능하고_반복_삭제는_최초_시각을_유지한다() {
        Long receiverId = createUser("수신자");
        Long id = saveNotification(receiverId);
        Notification notification = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        notification.read(NOW);
        LocalDateTime deletedAt = NOW.plusHours(1);

        notification.delete(deletedAt);
        flushAndClear();

        Notification deleted = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.isRead()).isTrue();
        assertThat(deleted).hasFieldOrPropertyWithValue("readAt", NOW)
                .hasFieldOrPropertyWithValue("deletedAt", deletedAt)
                .hasFieldOrPropertyWithValue("eventKey", EVENT_KEY)
                .hasFieldOrPropertyWithValue("content", CONTENT);

        deleted.delete(deletedAt.plusHours(1));
        flushAndClear();

        assertThat(notifications.findByIdAndReceiverId(id, receiverId).orElseThrow())
                .hasFieldOrPropertyWithValue("deletedAt", deletedAt);
    }

    @Test
    void 같은_수신자에게_동일_사건의_알림을_중복_저장할_수_없다() {
        Long receiverId = createUser("수신자");
        saveNotification(receiverId);

        assertThatThrownBy(() -> notifications.saveAndFlush(newNotification(receiverId)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_notification_receiver_event");
    }

    @Test
    void 같은_사건을_서로_다른_수신자에게_저장할_수_있다() {
        Long receiverId = createUser("수신자");
        Long otherId = createUser("다른 회원");

        Long firstId = saveNotification(receiverId);
        Long secondId = saveNotification(otherId);

        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(notifications.findByIdAndReceiverId(firstId, receiverId)).isPresent();
        assertThat(notifications.findByIdAndReceiverId(secondId, otherId)).isPresent();
    }

    @Test
    void 삭제한_알림의_사건도_동일_수신자에게_다시_생성할_수_없다() {
        Long receiverId = createUser("수신자");
        Long id = saveNotification(receiverId);
        Notification notification = notifications.findByIdAndReceiverId(id, receiverId).orElseThrow();
        notification.delete(NOW);
        flushAndClear();

        assertThatThrownBy(() -> notifications.saveAndFlush(newNotification(receiverId)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_notification_receiver_event");
    }

    @Test
    void 본인의_미삭제_알림_전체와_미읽음_개수만_집계한다() {
        Long receiverId = createUser("수신자");
        Long otherId = createUser("다른 회원");
        Notification read = notifications.save(Notification.create(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                NotificationTargetType.COMPANION_SEARCH, 42L, "matching-found:read", CONTENT));
        read.read(NOW);
        saveNotification(receiverId);
        Notification deletedUnread = notifications.save(Notification.create(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                NotificationTargetType.COMPANION_SEARCH, 42L, "matching-found:deleted-unread", CONTENT));
        deletedUnread.delete(NOW);
        Notification deletedRead = notifications.save(Notification.create(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                NotificationTargetType.COMPANION_SEARCH, 42L, "matching-found:deleted-read", CONTENT));
        deletedRead.read(NOW);
        deletedRead.delete(NOW);
        saveNotification(otherId);
        flushAndClear();

        assertThat(notifications.countVisibleByReceiverId(receiverId))
                .isEqualTo(new NotificationRepository.Counts(2, 1));
    }

    @Test
    void 알림이_없으면_전체와_미읽음_개수는_모두_0이다() {
        Long receiverId = createUser("수신자");

        assertThat(notifications.countVisibleByReceiverId(receiverId))
                .isEqualTo(new NotificationRepository.Counts(0, 0));
    }

    @Test
    void 이관된_예약_사건_키를_가진_기존_알림은_JPA로_복원할_수_있다() {
        Long receiverId = createUser("수신자");
        entityManager.createNativeQuery("""
                INSERT INTO notification (receiver_id, type, event_key, content, created_at, updated_at)
                VALUES (:receiverId, 'WELCOME', 'legacy:notification:100', '기존 알림', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """).setParameter("receiverId", receiverId).executeUpdate();
        flushAndClear();

        assertThat(notifications.findVisibleBefore(receiverId, null, 20, false)).singleElement()
                .hasFieldOrPropertyWithValue("eventKey", "legacy:notification:100")
                .hasFieldOrPropertyWithValue("content", "기존 알림");
    }

    @Test
    void 사건이_중복되면_최초_내용과_읽음_삭제_시각을_그대로_유지한다() {
        Long receiverId = createUser("멱등 수신자");
        Notification original = Notification.create(receiverId, NotificationType.WELCOME,
                null, null, "account-signup:idempotent", "최초 안내");
        notifications.saveIfAbsent(original, NOW);
        entityManager.clear();
        Notification saved = notifications.findVisibleBefore(receiverId, null, 20, false).getFirst();
        saved.read(NOW.plusMinutes(1));
        saved.delete(NOW.plusMinutes(2));
        flushAndClear();
        Notification duplicate = Notification.create(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                NotificationTargetType.COMPANION_SEARCH, 42L, "account-signup:idempotent", "변경된 안내");
        notifications.saveIfAbsent(duplicate, NOW.plusDays(1));
        entityManager.clear();
        assertThat(notifications.findByIdAndReceiverId(saved.getId(), receiverId).orElseThrow())
                .hasFieldOrPropertyWithValue("content", "최초 안내")
                .hasFieldOrPropertyWithValue("type", NotificationType.WELCOME)
                .hasFieldOrPropertyWithValue("targetType", null)
                .hasFieldOrPropertyWithValue("readAt", NOW.plusMinutes(1))
                .hasFieldOrPropertyWithValue("deletedAt", NOW.plusMinutes(2));
        assertThat(notifications.findVisibleBefore(receiverId, null, 20, false)).isEmpty();
    }

    @Test
    void 중복_외의_저장_오류는_무시하지_않는다() {
        Notification missingReceiver = Notification.create(Long.MAX_VALUE, NotificationType.WELCOME,
                null, null, "account-signup:missing", "없는 수신자");
        assertThatThrownBy(() -> notifications.saveIfAbsent(missingReceiver, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 미사용_과거_유형은_보존하되_목록_개수_상태변경에서_제외한다() {
        Long receiverId = createUser("과거 유형 수신자");
        Long currentId = saveNotification(receiverId);
        jdbc.update("""
                INSERT INTO notification
                    (receiver_id, type, target_type, target_id, event_key, content, created_at, updated_at)
                VALUES (?, 'TRIP_INVITE', 'TRIP', 7, 'retired:trip:7', '기존 안내', NOW(), NOW())
                """, receiverId);
        Long retiredId = jdbc.queryForObject("SELECT id FROM notification WHERE event_key = 'retired:trip:7'", Long.class);
        flushAndClear();
        assertThat(notifications.findVisibleBefore(receiverId, null, 20, false))
                .extracting(Notification::getId).containsExactly(currentId);
        assertThat(notifications.countVisibleByReceiverId(receiverId))
                .isEqualTo(new NotificationRepository.Counts(1, 1));
        assertThat(notifications.findByIdAndReceiverId(retiredId, receiverId)).isEmpty();
        assertThat(notifications.markAllVisibleUnreadAsRead(receiverId, NOW)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id = ? AND read_at IS NULL",
                Long.class, retiredId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT type FROM notification WHERE id = ?", String.class, retiredId))
                .isEqualTo("TRIP_INVITE");
    }

    private Long createUser(String nickname) {
        User user = User.builder().nickname(nickname).role(Role.USER).build();
        entityManager.persist(user);
        return user.getId();
    }

    private Long saveNotification(Long receiverId) {
        Notification saved = notifications.save(newNotification(receiverId));
        Long id = (Long) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(saved);
        flushAndClear();
        return id;
    }

    private Notification newNotification(Long receiverId) {
        return Notification.create(receiverId, NotificationType.COMPANION_MATCH_FOUND,
                NotificationTargetType.COMPANION_SEARCH, 42L, EVENT_KEY, CONTENT);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
