package com.daebangg.sayroam.user.domain.entity;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.user.domain.policy.UserProfilePolicy;
import com.daebangg.sayroam.user.domain.type.Gender;
import com.daebangg.sayroam.user.domain.type.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

// 테이블명 user는 예약어라 users를 쓴다
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대표 이메일. 탈퇴 시 NULL로 비워 재가입 시 UNIQUE 충돌을 막는다
    @Getter
    private String email;

    @Getter
    private LocalDateTime emailVerifiedAt;

    @Getter
    @Column(nullable = false, length = UserProfilePolicy.MAX_NICKNAME_LENGTH)
    private String nickname;

    @Getter
    @Column(length = UserProfilePolicy.MAX_PROFILE_IMAGE_URL_LENGTH)
    private String profileImageUrl;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Getter
    private Integer birthYear;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Getter
    private LocalDateTime deletedAt;

    @Getter
    @Column(nullable = false)
    private long authVersion;

    public static User verifiedEmailMember(String email, String nickname, LocalDateTime emailVerifiedAt) {
        User user = new User(Objects.requireNonNull(email), nickname,
                null, null, null, Role.USER);
        user.emailVerifiedAt = Objects.requireNonNull(emailVerifiedAt);
        return user;
    }

    public static User verifiedSocialMember(String email, String nickname, String profileImageUrl,
                                            LocalDateTime emailVerifiedAt) {
        User user = verifiedEmailMember(email, nickname, emailVerifiedAt);
        // 공급자의 빈 사진 값은 이미지 미제공을 의미한다.
        user.changeProfileImageUrl(profileImageUrl == null || profileImageUrl.isBlank()
                ? null : profileImageUrl);
        return user;
    }

    public boolean allowsAccountChange(String verifiedEmail, long expectedAuthVersion) {
        return deletedAt == null && emailVerifiedAt != null && email != null
                && email.equals(verifiedEmail) && authVersion == expectedAuthVersion;
    }

    /**
     * 자격 정보 변경과 같은 트랜잭션에서 호출한다.
     */
    public void invalidateLogins() {
        authVersion = Math.incrementExact(authVersion);
    }

    /**
     * 회원 ID는 공유 기록의 참조용으로 남기고 계정 프로필과 권한을 정리한다.
     */
    public void withdraw(LocalDateTime withdrawnAt) {
        Objects.requireNonNull(withdrawnAt, "withdrawnAt");
        if (deletedAt != null) {
            throw new IllegalStateException("Account is already withdrawn");
        }
        invalidateLogins();
        deletedAt = withdrawnAt;
        email = null;
        emailVerifiedAt = null;
        nickname = "탈퇴한 회원";
        profileImageUrl = null;
        gender = null;
        birthYear = null;
        role = Role.USER;
    }

    public void changeNickname(String nickname) {
        this.nickname = UserProfilePolicy.normalizeNickname(nickname);
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = UserProfilePolicy.normalizeProfileImageUrl(profileImageUrl);
    }

    public void changeGender(Gender gender) {
        this.gender = gender;
    }

    public void changeBirthYear(Integer birthYear) {
        this.birthYear = UserProfilePolicy.validateBirthYear(birthYear);
    }

    @Builder
    private User(String email, String nickname, String profileImageUrl, Gender gender, Integer birthYear, Role role) {
        this.email = email;
        this.nickname = UserProfilePolicy.normalizeNickname(nickname);
        this.profileImageUrl = UserProfilePolicy.normalizeProfileImageUrl(profileImageUrl);
        this.gender = gender;
        this.birthYear = UserProfilePolicy.validateBirthYear(birthYear);
        this.role = role;
    }

}
