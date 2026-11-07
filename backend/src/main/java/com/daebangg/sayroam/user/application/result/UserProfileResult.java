package com.daebangg.sayroam.user.application.result;

import com.daebangg.sayroam.user.domain.type.Gender;
import com.daebangg.sayroam.user.domain.type.Role;
import com.daebangg.sayroam.user.domain.entity.User;

/** 트랜잭션 안에서 복사한 회원 프로필. JPA 엔티티를 외부에 노출하지 않는다. */
public record UserProfileResult(
        long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Gender gender,
        Integer birthYear,
        Role role) {

    public static UserProfileResult from(User user) {
        return new UserProfileResult(user.getId(), user.getEmail(), user.getNickname(),
                user.getProfileImageUrl(), user.getGender(), user.getBirthYear(), user.getRole());
    }
}
