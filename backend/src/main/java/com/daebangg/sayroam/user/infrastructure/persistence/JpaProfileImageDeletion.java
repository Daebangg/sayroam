package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.application.port.out.ProfileImageDeletion;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaProfileImageDeletion implements ProfileImageDeletion {
    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteForUser(long userId) {
        entityManager.createQuery("delete from UserProfileImage p where p.userId = :userId")
                .setParameter("userId", userId).executeUpdate();
    }
}
