package com.daebangg.sayroam.notification.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.notification.domain.error.NotificationErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notification", uniqueConstraints =
@UniqueConstraint(name = "uk_notification_receiver_event", columnNames = {"receiver_id", "event_key"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    private static final int MAX_EVENT_KEY_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 255;
    private static final String LEGACY_EVENT_KEY_PREFIX = "legacy:notification:";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Getter
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Getter
    private NotificationTargetType targetType;

    // 대상이 여러 테이블에 걸쳐 있고 삭제돼도 알림은 남아야 해서 FK 없이 값으로만 둔다
    @Getter
    private Long targetId;

    @Column(name = "event_key", nullable = false, length = MAX_EVENT_KEY_LENGTH, updatable = false)
    private String eventKey;

    // 만든 시점의 문구를 저장해 닉네임 변경·탈퇴와 무관하게 그대로 보여준다
    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    @Getter
    private String content;

    private LocalDateTime readAt;

    private LocalDateTime deletedAt;


    private Notification(
            Long receiverId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String eventKey,
            String content
    ) {
        validateReceiver(receiverId);
        validateType(type);
        validateContent(content);
        validateEventKey(eventKey);
        validateTarget(targetType, targetId);

        this.receiverId = receiverId;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.eventKey = eventKey;
        this.content = content;
    }

    public static Notification create(Long receiverId, NotificationType type, NotificationTargetType targetType, Long targetId, String eventKey, String content) {
        return new Notification(receiverId,
                type,
                targetType,
                targetId,
                eventKey,
                content);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void read(LocalDateTime now) {
        ensureNotDeleted();
        Objects.requireNonNull(now, "Read time is required.");

        if (isRead()) {
            return;
        }

        this.readAt = now;
    }

    public void unread() {
        ensureNotDeleted();
        this.readAt = null;
    }

    public void delete(LocalDateTime now) {
        if (isDeleted()) {
            return;
        }
        Objects.requireNonNull(now, "Deleted time is required");
        this.deletedAt = now;
    }

    private static void validateReceiver(Long receiverId) {
        if (receiverId == null || receiverId <= 0) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_RECEIVER_INVALID
            );
        }
    }

    private static void validateType(NotificationType type) {
        if (type == null) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_TYPE_REQUIRED
            );
        }
    }

    private static void validateContent(String content) {
        if (content == null
                || content.isBlank()
                || content.codePointCount(0, content.length()) > MAX_CONTENT_LENGTH) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_CONTENT_INVALID
            );
        }
    }

    private static void validateEventKey(String eventKey) {
        if (eventKey == null
                || eventKey.isBlank()
                || !eventKey.equals(eventKey.strip())
                || eventKey.regionMatches(true, 0, LEGACY_EVENT_KEY_PREFIX, 0, LEGACY_EVENT_KEY_PREFIX.length())
                || eventKey.codePointCount(0, eventKey.length()) > MAX_EVENT_KEY_LENGTH) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_EVENT_KEY_INVALID
            );
        }
    }

    private static void validateTarget(
            NotificationTargetType targetType,
            Long targetId
    ) {
        boolean hasTargetType = targetType != null;
        boolean hasTargetId = targetId != null;

        if (hasTargetType != hasTargetId) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_TARGET_INVALID
            );
        }

        if (hasTargetId && targetId <= 0) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_TARGET_INVALID
            );
        }
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new BusinessException(
                    NotificationErrorCode.NOTIFICATION_DELETED
            );
        }
    }
}
