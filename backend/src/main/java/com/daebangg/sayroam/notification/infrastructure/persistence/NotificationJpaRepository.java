package com.daebangg.sayroam.notification.infrastructure.persistence;

import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            int size
    ) {
        return findVisibleBefore(receiverId, beforeId, Limit.of(size));
    }

    @Query("""
            select n
            from Notification n
            where n.receiverId = :receiverId
              and n.deletedAt is null
              and (:beforeId is null or n.id < :beforeId)
            order by n.id desc
            """)
    List<Notification> findVisibleBefore(
            @Param("receiverId") Long receiverId,
            @Param("beforeId") Long beforeId,
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
}
