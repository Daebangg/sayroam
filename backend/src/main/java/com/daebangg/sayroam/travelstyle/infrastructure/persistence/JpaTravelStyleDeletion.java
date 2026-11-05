package com.daebangg.sayroam.travelstyle.infrastructure.persistence;

import com.daebangg.sayroam.travelstyle.application.port.out.TravelStyleDeletion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaTravelStyleDeletion implements TravelStyleDeletion {
    private final EntityManager entityManager;

    public JpaTravelStyleDeletion(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForUser(long userId) {
        entityManager.createQuery("delete from UserTravelStyle s where s.userId = :userId")
                .setParameter("userId", userId).executeUpdate();
    }
}
