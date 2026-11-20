package com.daebangg.sayroam.notification.infrastructure.persistence;

import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationJpaRepository extends NotificationRepository, JpaRepository<Notification, Long> {
    @Override
    <S extends Notification> S save(S notification);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    @Override
    default List<Notification> findVisibleBefore(
            Long receiverId,
            Long beforeId,
            int size,
            boolean unreadOnly
    ) {
        return findVisibleBefore(receiverId, beforeId, unreadOnly, Limit.of(size));
    }

    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and n.deletedAt is null
              and (:unreadOnly = false or n.readAt is null)
              and (:beforeId is null or n.id < :beforeId)
            order by n.id desc
            """)
    List<Notification> findVisibleBefore(
            @Param("receiverId") Long receiverId,
            @Param("beforeId") Long beforeId,
            @Param("unreadOnly") boolean unreadOnly,
            Limit limit
    );

    @Override
    @Query("""
            select new com.daebangg.sayroam.notification.domain.NotificationRepository$Counts(
                count(n.id), count(n.id) - count(n.readAt))
            from Notification n
            where n.receiverId = :receiverId
              and n.deletedAt is null
            """)
    Counts countVisibleByReceiverId(@Param("receiverId") Long receiverId);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :readAt, n.updatedAt = :readAt
            where n.receiverId = :receiverId
              and n.deletedAt is null
              and n.readAt is null
            """)
    int markAllVisibleUnreadAsRead(@Param("receiverId") Long receiverId, @Param("readAt") LocalDateTime readAt);
}
