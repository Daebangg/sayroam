package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대표 이메일. 탈퇴 시 NULL로 비워 재가입 시 UNIQUE 충돌을 막는다
    private String email;

    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private long authVersion;

    public static User verifiedEmailMember(String email, String nickname, LocalDateTime emailVerifiedAt) {
        User user = new User(Objects.requireNonNull(email), Objects.requireNonNull(nickname),
                null, null, null, Role.USER);
        user.emailVerifiedAt = Objects.requireNonNull(emailVerifiedAt);
        return user;
    }

    public Long getId() {
        return id;
    }

    public static User verifiedSocialMember(String email, String nickname, String profileImageUrl,
                                            LocalDateTime emailVerifiedAt) {
        User user = verifiedEmailMember(email, nickname, emailVerifiedAt);
        user.profileImageUrl = profileImageUrl;
        return user;
    }

    public boolean allowsAccountChange(String verifiedEmail, long expectedAuthVersion) {
        return deletedAt == null && emailVerifiedAt != null && email != null
                && email.equals(verifiedEmail) && authVersion == expectedAuthVersion;
    }

    /** 자격 정보 변경과 같은 트랜잭션에서 호출한다. */
    public void invalidateLogins() {
        authVersion = Math.incrementExact(authVersion);
    }

    /** 회원 ID는 공유 기록의 참조용으로 남기고 계정 프로필과 권한을 정리한다. */
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

    @Builder
    private User(String email, String nickname, String profileImageUrl, Gender gender, Integer birthYear, Role role) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.birthYear = birthYear;
        this.role = role;
    }

}
