package com.daebangg.sayroam.user.application.command;

import com.daebangg.sayroam.user.domain.type.Gender;

import java.util.Objects;

/** HTTP 형식에 의존하지 않는 프로필 부분 수정 입력. */
public record UpdateUserProfileCommand(
        FieldUpdate<String> nickname,
        FieldUpdate<String> profileImageUrl,
        FieldUpdate<Gender> gender,
        FieldUpdate<Integer> birthYear) {

    public UpdateUserProfileCommand {
        Objects.requireNonNull(nickname, "nickname");
        Objects.requireNonNull(profileImageUrl, "profileImageUrl");
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(birthYear, "birthYear");
    }

    /** present=false는 기존 값 유지, present=true와 value=null은 삭제 요청이다. */
    public record FieldUpdate<T>(boolean present, T value) {

        public FieldUpdate {
            if (!present && value != null) {
                throw new IllegalArgumentException("An absent field cannot carry a value");
            }
        }

        public static <T> FieldUpdate<T> absent() {
            return new FieldUpdate<>(false, null);
        }

        public static <T> FieldUpdate<T> of(T value) {
            return new FieldUpdate<>(true, value);
        }
    }
}
