package com.daebangg.sayroam.notification.infrastructure.persistence;

import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationRepository;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
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
    // 이전 DB ENUM 값은 보존하되 현재 서비스에서 지원하는 알림만 노출한다.
    String CURRENT_TYPES = " n.type in (WELCOME, COMPANION_MATCH_FOUND) ";

    @Override
    <S extends Notification> S save(S notification);

    @Override
    @Modifying
    @Query(value = """
            INSERT INTO notification
                (receiver_id, type, target_type, target_id, event_key, content, created_at, updated_at)
            VALUES (:#{#notification.receiverId}, :#{#notification.type.name()},
                    :#{#notification.targetType == null ? null : #notification.targetType.name()},
                    :#{#notification.targetId}, :#{#notification.eventKey}, :#{#notification.content}, :now, :now)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void saveIfAbsent(@Param("notification") Notification notification, @Param("now") LocalDateTime now);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id and n.receiverId = :receiverId and " + CURRENT_TYPES)
    Optional<Notification> findByIdAndReceiverId(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from Notification n
            where n.receiverId = :receiverId and n.type = :type
              and n.targetType = :targetType and n.targetId = :targetId
              and n.readAt is null and n.deletedAt is null
            order by n.id limit 1
            """)
    Optional<Notification> findUnreadByTarget(@Param("receiverId") Long receiverId,
                                             @Param("type") NotificationType type,
                                             @Param("targetType") NotificationTargetType targetType,
                                             @Param("targetId") Long targetId);

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
            where
            """ + CURRENT_TYPES + """
              and n.receiverId = :receiverId
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
            where
            """ + CURRENT_TYPES + """
              and n.receiverId = :receiverId
              and n.deletedAt is null
            """)
    Counts countVisibleByReceiverId(@Param("receiverId") Long receiverId);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :readAt, n.updatedAt = :readAt
            where
            """ + CURRENT_TYPES + """
              and n.receiverId = :receiverId
              and n.deletedAt is null
              and n.readAt is null
            """)
    int markAllVisibleUnreadAsRead(@Param("receiverId") Long receiverId, @Param("readAt") LocalDateTime readAt);
}
