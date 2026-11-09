package com.daebangg.sayroam.global.validation;

public final class InputText {
    private InputText() { }

    /** JavaScript trim과 같은 공백·구분자·BOM만 앞뒤에서 제거한다. 내부 문자는 유지한다. */
    public static String trim(String value) {
        return value == null ? null : value.replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$", "");
    }
}
