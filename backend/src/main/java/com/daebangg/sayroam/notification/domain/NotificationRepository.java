package com.daebangg.sayroam.notification.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    <S extends Notification> S save(S notification);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findVisibleBefore(
            Long receiverId,
            Long beforeId,
            int size
    );

    Counts countVisibleByReceiverId(Long receiverId);

    record Counts(long totalCount, long unreadCount) {
    }
}
