package com.daebangg.sayroam.auth.domain;

import java.util.Locale;
import com.daebangg.sayroam.global.validation.InputText;

/** 가입·로그인 구현에서도 같은 이메일 비교 기준을 사용한다. */
public final class EmailAddress {
    public static final int MAX_LENGTH = 254;
    private static final String ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final String LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
    public static final String PATTERN = "^(?=.{1,254}$)(?=[^@]{1,64}@)" + ATOM + "(?:\\." + ATOM + ")*@"
            + LABEL + "(?:\\." + LABEL + ")+$";
    private EmailAddress() {
    }

    public static String normalize(String email) {
        return InputText.trim(email).toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        return email != null && InputText.trim(email).matches(PATTERN);
    }
}
