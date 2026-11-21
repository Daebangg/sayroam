package com.daebangg.sayroam.notification.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.notification.application.dto.CreateNotificationCommand;
import com.daebangg.sayroam.notification.application.dto.NotificationListQuery;
import com.daebangg.sayroam.notification.application.dto.NotificationResult;
import com.daebangg.sayroam.notification.application.dto.NotificationSummaryResult;
import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationRepository;
import com.daebangg.sayroam.notification.domain.error.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional
    public void create(CreateNotificationCommand command) {
        Notification notification = Notification.create(
                command.receiverId(),
                command.type(),
                command.targetType(),
                command.targetId(),
                command.eventKey(),
                command.content()
        );
        notifications.save(notification);
    }

    @Transactional
    public void createIfAbsent(CreateNotificationCommand command) {
        Notification notification = Notification.create(command.receiverId(), command.type(), command.targetType(),
                command.targetId(), command.eventKey(), command.content());
        // 이미 알린 작업은 읽음·삭제 여부와 관계없이 다시 만들지 않는다.
        notifications.saveIfAbsent(notification, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public List<NotificationResult> findAll(NotificationListQuery query) {
        return notifications.findVisibleBefore(
                        query.receiverId(),
                        query.beforeId(),
                        query.size(),
                        query.unreadOnly()
                )
                .stream()
                .map(NotificationResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResult getSummary(Long userId) {
        return NotificationSummaryResult.from(notifications.countVisibleByReceiverId(userId));
    }

    @Transactional
    public void read(Long userId, Long notificationId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notification.read(LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void readAll(Long userId) {
        notifications.markAllVisibleUnreadAsRead(userId, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void unread(Long userId, Long notificationId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notification.unread();
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notification.delete(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Notification getOwnedNotification(Long notificationId, Long userId) {
        return notifications.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(
                        NotificationErrorCode.NOTIFICATION_NOT_FOUND
                ));
    }
}
