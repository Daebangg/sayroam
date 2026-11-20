package com.daebangg.sayroam.notification.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.notification.domain.error.NotificationErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {
    private static final Long RECEIVER_ID = 7L;
    private static final Long TARGET_ID = 42L;
    private static final String EVENT_KEY = "trip-invite:42";
    private static final String CONTENT = "여행에 초대되었습니다.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Test
    void 생성한_알림은_입력값을_보존하고_미읽음_미삭제_상태로_시작한다() {
        Notification notification = createNotification();

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.isDeleted()).isFalse();
        assertThat(notification).extracting("receiverId", "type", "targetType", "targetId", "eventKey", "content")
                .containsExactly(RECEIVER_ID, NotificationType.TRIP_INVITE,
                        NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, CONTENT);
    }

    @Test
    void 연결_대상_타입과_ID를_모두_생략할_수_있다() {
        Notification notification = Notification.create(
                RECEIVER_ID, NotificationType.TRIP_INVITE, null, null, EVENT_KEY, CONTENT);

        assertThat(notification).hasFieldOrPropertyWithValue("targetType", null)
                .hasFieldOrPropertyWithValue("targetId", null);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void 수신자_ID가_없거나_양수가_아니면_생성을_거부한다(Long receiverId) {
        assertThatThrownBy(() -> Notification.create(receiverId, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, CONTENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_RECEIVER_INVALID);
    }

    @Test
    void 알림_종류가_없으면_생성을_거부한다() {
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, null,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, CONTENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_TYPE_REQUIRED);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n", "\u2003"})
    void 내용이_없거나_공백뿐이면_생성을_거부한다(String content) {
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, content))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_CONTENT_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"가", "😀"})
    void 내용은_이모지를_포함해_255자까지_허용하고_256자는_거부한다(String character) {
        String content = character.repeat(255);

        Notification notification = Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, content);

        assertThat(notification).hasFieldOrPropertyWithValue("content", content);
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, character.repeat(256)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_CONTENT_INVALID);
    }

    @Test
    void 내용의_앞뒤_공백과_줄바꿈을_유지한다() {
        String content = "  여행에\n초대되었습니다.  ";

        Notification notification = Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, content);

        assertThat(notification).hasFieldOrPropertyWithValue("content", content);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n", " event:1", "event:1 ", "\nevent:1", "event:1\t"})
    void 사건_식별값이_없거나_공백뿐이거나_앞뒤_공백이_있으면_생성을_거부한다(String eventKey) {
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, eventKey, CONTENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_EVENT_KEY_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "😀"})
    void 사건_식별값은_150자까지_허용하고_151자는_거부한다(String character) {
        String eventKey = character.repeat(150);

        Notification notification = Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, eventKey, CONTENT);

        assertThat(notification).hasFieldOrPropertyWithValue("eventKey", eventKey);
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, character.repeat(151), CONTENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_EVENT_KEY_INVALID);
    }

    @ParameterizedTest
    @MethodSource("invalidTargets")
    void 연결_대상의_짝이_맞지_않거나_ID가_양수가_아니면_생성을_거부한다(
            NotificationTargetType targetType, Long targetId) {
        assertThatThrownBy(() -> Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                targetType, targetId, EVENT_KEY, CONTENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_TARGET_INVALID);
    }

    private static Stream<Arguments> invalidTargets() {
        return Stream.of(
                Arguments.of(NotificationTargetType.TRIP, null),
                Arguments.of(null, TARGET_ID),
                Arguments.of(NotificationTargetType.TRIP, 0L),
                Arguments.of(NotificationTargetType.TRIP, -1L),
                Arguments.of(null, 0L),
                Arguments.of(null, -1L));
    }

    @Test
    void 읽음_처리를_반복해도_최초_읽은_시각을_유지한다() {
        Notification notification = createNotification();

        notification.read(NOW);
        notification.read(NOW.plusHours(1));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification).hasFieldOrPropertyWithValue("readAt", NOW);
        assertThat(notification.isDeleted()).isFalse();
    }

    @Test
    void 미읽음으로_변경한_후_다시_읽으면_새로_읽은_시각을_기록한다() {
        Notification notification = createNotification();
        notification.read(NOW);

        notification.unread();

        assertThat(notification.isRead()).isFalse();
        assertThat(notification).hasFieldOrPropertyWithValue("readAt", null);

        LocalDateTime rereadAt = NOW.plusHours(1);
        notification.read(rereadAt);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification).hasFieldOrPropertyWithValue("readAt", rereadAt);
    }

    @Test
    void 이미_미읽음인_알림을_반복해서_미읽음_처리할_수_있다() {
        Notification notification = createNotification();

        notification.unread();
        notification.unread();

        assertThat(notification.isRead()).isFalse();
        assertThat(notification.isDeleted()).isFalse();
    }

    @Test
    void 읽은_시각이_없으면_읽음_처리를_거부하고_미읽음을_유지한다() {
        Notification notification = createNotification();

        assertThatThrownBy(() -> notification.read(null)).isInstanceOf(NullPointerException.class);
        assertThat(notification.isRead()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 삭제는_읽음_상태와_내용_대상_사건_식별값을_유지한다(boolean readBeforeDeletion) {
        Notification notification = createNotification();
        if (readBeforeDeletion) {
            notification.read(NOW);
        }
        LocalDateTime deletedAt = NOW.plusHours(1);

        notification.delete(deletedAt);

        assertThat(notification.isDeleted()).isTrue();
        assertThat(notification.isRead()).isEqualTo(readBeforeDeletion);
        assertThat(notification).hasFieldOrPropertyWithValue("deletedAt", deletedAt)
                .hasFieldOrPropertyWithValue("readAt", readBeforeDeletion ? NOW : null);
        assertThat(notification).extracting("receiverId", "type", "targetType", "targetId", "eventKey", "content")
                .containsExactly(RECEIVER_ID, NotificationType.TRIP_INVITE,
                        NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, CONTENT);
    }

    @Test
    void 삭제를_반복해도_최초_삭제_시각을_유지한다() {
        Notification notification = createNotification();

        notification.delete(NOW);
        notification.delete(NOW.plusHours(1));

        assertThat(notification.isDeleted()).isTrue();
        assertThat(notification).hasFieldOrPropertyWithValue("deletedAt", NOW);
    }

    @Test
    void 삭제_시각이_없으면_삭제를_거부하고_미삭제를_유지한다() {
        Notification notification = createNotification();

        assertThatThrownBy(() -> notification.delete(null)).isInstanceOf(NullPointerException.class);
        assertThat(notification.isDeleted()).isFalse();
    }

    @Test
    void 삭제한_미읽음_알림은_읽음으로_변경할_수_없다() {
        Notification notification = createNotification();
        notification.delete(NOW);

        assertThatThrownBy(() -> notification.read(NOW.plusHours(1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_DELETED);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification).hasFieldOrPropertyWithValue("deletedAt", NOW);
    }

    @Test
    void 삭제한_읽음_알림은_미읽음으로_변경할_수_없다() {
        Notification notification = createNotification();
        notification.read(NOW);
        LocalDateTime deletedAt = NOW.plusHours(1);
        notification.delete(deletedAt);

        assertThatThrownBy(notification::unread)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.NOTIFICATION_DELETED);
        assertThat(notification.isRead()).isTrue();
        assertThat(notification).hasFieldOrPropertyWithValue("readAt", NOW)
                .hasFieldOrPropertyWithValue("deletedAt", deletedAt);
    }

    private Notification createNotification() {
        return Notification.create(RECEIVER_ID, NotificationType.TRIP_INVITE,
                NotificationTargetType.TRIP, TARGET_ID, EVENT_KEY, CONTENT);
    }
}
