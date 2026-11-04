package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.application.port.out.LoginMethodDeletion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaLoginMethodDeletion implements LoginMethodDeletion {
    private final EntityManager entityManager;

    public JpaLoginMethodDeletion(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForUser(long userId) {
        entityManager.createQuery("delete from UserCredential c where c.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
        entityManager.createQuery("delete from UserSocialAccount a where a.user.id = :userId")
                .setParameter("userId", userId).executeUpdate();
    }
}
