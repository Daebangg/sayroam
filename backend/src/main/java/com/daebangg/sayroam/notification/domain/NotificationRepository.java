package com.daebangg.sayroam.notification.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    <S extends Notification> S save(S notification);

    void saveIfAbsent(Notification notification, LocalDateTime now);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findVisibleBefore(
            Long receiverId,
            Long beforeId,
            int size,
            boolean unreadOnly
    );

    Counts countVisibleByReceiverId(Long receiverId);

    int markAllVisibleUnreadAsRead(Long receiverId, LocalDateTime readAt);

    record Counts(long totalCount, long unreadCount) {
    }
}
