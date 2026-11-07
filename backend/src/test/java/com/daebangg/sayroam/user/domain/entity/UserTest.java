package com.daebangg.sayroam.user.domain.entity;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import com.daebangg.sayroam.user.domain.type.Gender;
import com.daebangg.sayroam.user.domain.type.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {
    private static final String EMAIL = "user@example.com";
    private static final LocalDateTime VERIFIED_AT = LocalDateTime.of(2026, 11, 7, 10, 0);

    @Test
    void 회원_생성과_수정에서_닉네임의_앞뒤_공백을_제거한다() {
        User user = User.verifiedEmailMember(EMAIL, "　 여행자 　", VERIFIED_AT);

        assertThat(user.getNickname()).isEqualTo("여행자");
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getAuthVersion()).isZero();

        user.changeNickname("  제주 여행자  ");

        assertThat(user.getNickname()).isEqualTo("제주 여행자");
    }

    @ParameterizedTest
    @MethodSource("invalidNicknames")
    void 잘못된_닉네임은_생성과_수정에서_같은_오류로_거절한다(
            String nickname, UserErrorCode expectedCode) {
        User user = member();

        assertThatThrownBy(() -> user.changeNickname(nickname))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(expectedCode));
        assertThat(user.getNickname()).isEqualTo("여행자");

        assertThatThrownBy(() -> User.verifiedEmailMember(EMAIL, nickname, VERIFIED_AT))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(expectedCode));
        assertThatThrownBy(() -> User.builder().nickname(nickname).role(Role.USER).build())
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(expectedCode));
    }

    static Stream<Arguments> invalidNicknames() {
        return Stream.of(
                Arguments.of(null, UserErrorCode.USER_NICKNAME_REQUIRED),
                Arguments.of("", UserErrorCode.USER_NICKNAME_REQUIRED),
                Arguments.of(" \t\n　", UserErrorCode.USER_NICKNAME_REQUIRED),
                Arguments.of("가".repeat(51), UserErrorCode.USER_NICKNAME_TOO_LONG),
                Arguments.of("여행\n자", UserErrorCode.USER_NICKNAME_INVALID),
                Arguments.of("여행\u007F자", UserErrorCode.USER_NICKNAME_INVALID)
        );
    }

    @Test
    void 닉네임_길이는_기존_가입_검증과_같은_기준으로_50자까지_허용한다() {
        User user = member();

        user.changeNickname("  " + "가".repeat(50) + "  ");
        assertThat(user.getNickname()).isEqualTo("가".repeat(50));

        user.changeNickname("😀".repeat(25));
        assertThat(user.getNickname()).hasSize(50);

        assertThatThrownBy(() -> user.changeNickname("😀".repeat(25) + "가"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_NICKNAME_TOO_LONG));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/profile.png",
            "http://example.com:8080/profile.png?size=100",
            "HTTPS://example.com/profile.png"
    })
    void 이미지_URL은_HTTP와_HTTPS를_허용하고_공백을_정리한다(String imageUrl) {
        User user = member();

        user.changeProfileImageUrl("  " + imageUrl + "  ");

        assertThat(user.getProfileImageUrl()).isEqualTo(imageUrl);
        assertThat(User.verifiedSocialMember(EMAIL, "여행자", imageUrl, VERIFIED_AT)
                .getProfileImageUrl()).isEqualTo(imageUrl);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n　"})
    void 소셜_가입에서_빈_이미지는_미설정으로_처리한다(String imageUrl) {
        User user = User.verifiedSocialMember(EMAIL, "여행자", imageUrl, VERIFIED_AT);

        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);
    }

    @ParameterizedTest
    @MethodSource("invalidImageUrls")
    void 잘못된_이미지_URL은_거절하고_기존_이미지는_유지한다(String imageUrl) {
        User user = member();
        user.changeProfileImageUrl("https://example.com/original.png");

        assertThatThrownBy(() -> user.changeProfileImageUrl(imageUrl))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID));
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/original.png");
        assertThatThrownBy(() -> User.builder().nickname("여행자")
                .profileImageUrl(imageUrl).role(Role.USER).build())
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID));
    }

    @ParameterizedTest
    @MethodSource("malformedImageUrls")
    void 소셜_가입에서도_잘못된_이미지_URL은_거절한다(String imageUrl) {
        assertThatThrownBy(() -> User.verifiedSocialMember(EMAIL, "여행자", imageUrl, VERIFIED_AT))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID));
    }

    static Stream<String> invalidImageUrls() {
        return Stream.concat(Stream.of("", "   "), malformedImageUrls());
    }

    static Stream<String> malformedImageUrls() {
        return Stream.of("/profile.png", "ftp://example.com/profile.png", "https://",
                "https://example.com/a b.png", "https://example.com/%invalid",
                "https://user:password@example.com/profile.png", "https://example.com:0/profile.png",
                "https://example.com:65536/profile.png");
    }

    @Test
    void 이미지_URL은_공백을_제거한_길이로_500자까지_허용한다() {
        String prefix = "https://example.com/";
        String boundaryUrl = prefix + "a".repeat(500 - prefix.length());
        User user = member();

        user.changeProfileImageUrl("  " + boundaryUrl + "  ");
        assertThat(user.getProfileImageUrl()).isEqualTo(boundaryUrl);

        assertThatThrownBy(() -> user.changeProfileImageUrl(boundaryUrl + "a"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_PROFILE_IMAGE_URL_TOO_LONG));
        assertThat(user.getProfileImageUrl()).isEqualTo(boundaryUrl);
    }

    @Test
    void 선택_프로필은_설정하고_변경하고_null로_제거할_수_있다() {
        User user = User.builder().nickname("여행자").role(Role.USER)
                .profileImageUrl("  https://example.com/first.png  ")
                .gender(Gender.MALE).birthYear(2000).build();

        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/first.png");
        assertThat(user.getGender()).isEqualTo(Gender.MALE);
        assertThat(user.getBirthYear()).isEqualTo(2000);

        user.changeProfileImageUrl("https://example.com/second.png");
        user.changeGender(Gender.FEMALE);
        user.changeBirthYear(1999);

        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/second.png");
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getBirthYear()).isEqualTo(1999);

        user.changeProfileImageUrl(null);
        user.changeGender(null);
        user.changeBirthYear(null);

        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getGender()).isNull();
        assertThat(user.getBirthYear()).isNull();
        assertThat(user.getNickname()).isEqualTo("여행자");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getAuthVersion()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void 양수가_아닌_출생연도는_생성과_수정에서_거절한다(int birthYear) {
        assertInvalidBirthYear(birthYear);
    }

    @Test
    void 미래_출생연도는_거절하고_현재_연도까지_허용한다() {
        int currentYear = Year.now().getValue();
        User user = member();

        user.changeBirthYear(currentYear);
        assertThat(user.getBirthYear()).isEqualTo(currentYear);

        assertInvalidBirthYear(currentYear + 1);
    }

    @Test
    void 탈퇴하면_프로필을_정리하고_기존_인증_버전을_무효화한다() {
        User user = User.verifiedSocialMember(EMAIL, "여행자",
                "https://example.com/profile.png", VERIFIED_AT);
        user.changeGender(Gender.FEMALE);
        user.changeBirthYear(2000);
        LocalDateTime withdrawnAt = VERIFIED_AT.plusDays(1);
        assertThat(user.allowsAccountChange(EMAIL, 0)).isTrue();

        user.withdraw(withdrawnAt);

        assertThat(user.getDeletedAt()).isEqualTo(withdrawnAt);
        assertThat(user.getEmail()).isNull();
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getNickname()).isEqualTo("탈퇴한 회원");
        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getGender()).isNull();
        assertThat(user.getBirthYear()).isNull();
        assertThat(user.getAuthVersion()).isEqualTo(1);
        assertThat(user.allowsAccountChange(EMAIL, 0)).isFalse();
    }

    private static void assertInvalidBirthYear(int birthYear) {
        User user = member();
        user.changeBirthYear(2000);

        assertThatThrownBy(() -> user.changeBirthYear(birthYear))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_BIRTH_YEAR_INVALID));
        assertThat(user.getBirthYear()).isEqualTo(2000);

        assertThatThrownBy(() -> User.builder().nickname("여행자")
                .birthYear(birthYear).role(Role.USER).build())
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isSameAs(UserErrorCode.USER_BIRTH_YEAR_INVALID));
    }

    private static User member() {
        return User.verifiedEmailMember(EMAIL, "여행자", VERIFIED_AT);
    }
}
