package com.daebangg.sayroam.user.application.dto;

import com.daebangg.sayroam.user.domain.Gender;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import java.time.LocalDateTime;
import java.time.LocalDate;

/** 트랜잭션 안에서 복사한 회원 프로필. JPA 엔티티를 외부에 노출하지 않는다. */
public record UserProfileResult(
        long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Gender gender,
        Integer birthYear,
        Role role,
        LocalDateTime createdAt,
        LocalDate birthDate) {

    public static UserProfileResult from(User user) {
        return new UserProfileResult(user.getId(), user.getEmail(), user.getNickname(),
                user.getProfileImageUrl(), user.getGender(), user.getBirthYear(), user.getRole(), user.getCreatedAt(), user.getBirthDate());
    }
}
