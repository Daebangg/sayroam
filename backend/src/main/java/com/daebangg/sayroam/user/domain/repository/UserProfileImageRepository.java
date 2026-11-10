package com.daebangg.sayroam.user.domain.repository;

import com.daebangg.sayroam.user.domain.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
    @Query("select p from UserProfileImage p, User u where p.imageId = :imageId "
            + "and u.id = p.userId and u.deletedAt is null")
    Optional<UserProfileImage> findActiveImage(@Param("imageId") String imageId);
}
