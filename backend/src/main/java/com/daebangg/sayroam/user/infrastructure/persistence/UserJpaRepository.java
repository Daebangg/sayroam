package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.domain.SocialProvider;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import com.daebangg.sayroam.user.domain.UserRepository;
import jakarta.persistence.LockModeType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from UserCredential c where c.user.id = :userId")
    Optional<UserCredential> findCredentialForUpdate(@Param("userId") long userId);

    @Override
    default User saveNewAccount(User user) {
        try {
            return saveAndFlush(user);
        } catch (RuntimeException exception) {
            throw translateAccountConstraint(exception);
        }
    }

    @Override
    default void flushSocialAccount() {
        try {
            flush();
        } catch (RuntimeException exception) {
            throw translateAccountConstraint(exception);
        }
    }

    private static RuntimeException translateAccountConstraint(RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String name = violation.getConstraintName();
                String constraint = name.substring(name.lastIndexOf('.') + 1);
                return switch (constraint) {
                    case "uk_users_email" -> new UserRepository.DuplicateEmail(exception);
                    case "uk_user_social_account_provider_user", "uk_user_social_account_user_provider" ->
                            new UserRepository.DuplicateSocialAccount(exception);
                    default -> exception;
                };
            }
        }
        return exception;
    }

    record ProfileImageContent(byte[] profileImageContent) {}
}
