package com.daebangg.sayroam.auth.domain;

import java.util.Locale;

/** 가입·로그인 구현에서도 같은 이메일 비교 기준을 사용한다. */
public final class EmailAddress {
    private EmailAddress() {
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
