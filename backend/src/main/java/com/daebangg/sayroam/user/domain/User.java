package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    @Column(nullable = false, length = Nickname.MAX_LENGTH)
    private String nickname;

    @Getter
    @Column(length = ProfileImageUrl.MAX_LENGTH)
    private String profileImageUrl;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true)
    private UserProfileImage profileImage;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Getter
    private Integer birthYear;

    @Getter
    private LocalDate birthDate;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Getter
    private LocalDateTime deletedAt;

    @Getter
    @Column(nullable = false)
    private long authVersion;

    @Builder
    private User(String email, String nickname, String profileImageUrl, Gender gender,
                 Integer birthYear, Role role, LocalDate today) {
        this.email = email;
        changeNickname(nickname);
        this.profileImageUrl = profileImageUrl == null ? null : new ProfileImageUrl(profileImageUrl).value();
        this.gender = gender;
        changeBirthYear(birthYear, today);
        this.role = role;
    }

    public static User verifiedEmailMember(String email, String nickname, LocalDateTime emailVerifiedAt) {
        User user = new User(Objects.requireNonNull(email), nickname,
                null, null, null, Role.USER, null);
        user.emailVerifiedAt = Objects.requireNonNull(emailVerifiedAt);
        return user;
    }

    public static User verifiedSocialMember(String email, String nickname, String profileImageUrl,
                                            LocalDateTime emailVerifiedAt) {
        User user = verifiedEmailMember(email, nickname, emailVerifiedAt);
        // 공급자의 빈 사진 값은 이미지 미제공을 의미한다.
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            user.profileImageUrl = new ProfileImageUrl(profileImageUrl).value();
        }
        return user;
    }

    public boolean allowsAccountChange(String verifiedEmail, long expectedAuthVersion) {
        return isActiveWithAuthVersion(expectedAuthVersion) && emailVerifiedAt != null && email != null
                && email.equals(verifiedEmail);
    }

    public boolean isActiveWithAuthVersion(long expectedAuthVersion) {
        return deletedAt == null && authVersion == expectedAuthVersion;
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
        removeProfileImage();
        profileImageUrl = null;
        gender = null;
        birthYear = null;
        birthDate = null;
        role = Role.USER;
    }

    public void changeNickname(String nickname) {
        this.nickname = new Nickname(nickname).value();
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        String requestedUrl = profileImageUrl == null ? null : profileImageUrl.strip();
        if (requestedUrl != null && requestedUrl.startsWith(ProfileImageUrl.UPLOAD_PATH_PREFIX)
                && !requestedUrl.equals(this.profileImageUrl)) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID);
        }
        String nextUrl = requestedUrl == null ? null : new ProfileImageUrl(requestedUrl).value();
        if (!Objects.equals(this.profileImageUrl, nextUrl)) {
            removeProfileImage();
            this.profileImageUrl = nextUrl;
        }
    }

    public void uploadProfileImage(byte[] content) {
        if (profileImage == null) {
            profileImage = new UserProfileImage(this, content);
        } else {
            profileImage.replace(content);
        }
        profileImageUrl = profileImage.url();
    }

    public void changeGender(Gender gender) {
        this.gender = gender;
    }

    public void changeBirthYear(Integer birthYear, LocalDate today) {
        if (birthYear != null && (birthYear <= 0 || birthYear > today.getYear())) {
            throw new BusinessException(UserErrorCode.USER_BIRTH_YEAR_INVALID);
        }
        this.birthYear = birthYear;
        if (birthDate != null && (birthYear == null || birthDate.getYear() != birthYear)) {
            birthDate = null;
        }
    }

    public void changeBirthDate(LocalDate birthDate, LocalDate today) {
        if (birthDate != null && (birthDate.getYear() < 1 || birthDate.isAfter(today))) {
            throw new BusinessException(UserErrorCode.USER_BIRTH_DATE_INVALID);
        }
        this.birthDate = birthDate;
        this.birthYear = birthDate == null ? null : birthDate.getYear();
    }

    private void removeProfileImage() {
        // 지연 로딩된 기존 사진을 확인해야 JPA가 고아 엔티티 삭제를 감지한다.
        if (profileImage != null) {
            profileImage = null;
        }
    }
}
