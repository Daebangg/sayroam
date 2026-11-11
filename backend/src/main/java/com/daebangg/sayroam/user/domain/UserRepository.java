package com.daebangg.sayroam.user.domain;


import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByIdAndDeletedAtIsNull(Long userId);

    boolean hasPassword(long userId);

    List<SocialProvider> findSocialProviders(long userId);

    /** 비밀번호 변경·탈퇴와 같은 회원 행을 잠가 프로필 변경을 순서대로 처리한다. */
    Optional<User> findForUpdateByIdAndDeletedAtIsNull(long userId);

    Optional<byte[]> findActiveProfileImageContent(String imageId);

    void deleteLoginMethods(long userId);
}
