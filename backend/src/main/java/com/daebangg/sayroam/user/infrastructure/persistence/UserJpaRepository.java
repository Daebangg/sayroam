package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import com.daebangg.sayroam.user.domain.SocialProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends UserRepository, JpaRepository<User, Long> {
    @Override
    @Query("select count(c) > 0 from UserCredential c where c.user.id = :userId")
    boolean hasPassword(@Param("userId") long userId);

    @Override
    @Query("select s.provider from UserSocialAccount s where s.user.id = :userId order by s.provider")
    List<SocialProvider> findSocialProviders(@Param("userId") long userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByIdAndDeletedAtIsNull(long userId);

    @Override
    default Optional<byte[]> findActiveProfileImageContent(String imageId) {
        if (imageId == null) {
            return Optional.empty();
        }
        return findByProfileImage_ImageIdAndDeletedAtIsNull(imageId)
                .map(ProfileImageContent::profileImageContent);
    }

    Optional<ProfileImageContent> findByProfileImage_ImageIdAndDeletedAtIsNull(String imageId);

    @Override
    default void deleteLoginMethods(long userId) {
        deleteCredential(userId);
        deleteSocialAccounts(userId);
    }

    @Modifying
    @Query("delete from UserCredential c where c.user.id = :userId")
    void deleteCredential(@Param("userId") long userId);

    @Modifying
    @Query("delete from UserSocialAccount a where a.user.id = :userId")
    void deleteSocialAccounts(@Param("userId") long userId);

    record ProfileImageContent(byte[] profileImageContent) {}
}
