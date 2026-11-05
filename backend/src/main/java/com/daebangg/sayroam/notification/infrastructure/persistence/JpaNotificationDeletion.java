package com.daebangg.sayroam.notification.infrastructure.persistence;

import com.daebangg.sayroam.notification.application.port.out.NotificationDeletion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaNotificationDeletion implements NotificationDeletion {
    private final EntityManager entityManager;

    public JpaNotificationDeletion(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForUser(long userId) {
        entityManager.createQuery("delete from Notification n where n.receiverId = :userId")
                .setParameter("userId", userId).executeUpdate();
    }
}
