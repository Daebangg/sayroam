package com.daebangg.sayroam.global.validation;

public final class InputText {
    private InputText() { }

    /** JavaScript trim과 같은 공백·구분자·BOM만 앞뒤에서 제거한다. 내부 문자는 유지한다. */
    public static String trim(String value) {
        if (value == null) return null;
        int start = 0;
        int end = value.length();
        while (start < end && isTrimmable(value.charAt(start))) start++;
        while (end > start && isTrimmable(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isTrimmable(char character) {
        return character == ' ' || character == '\t' || character == '\n' || character == '\u000B'
                || character == '\f' || character == '\r' || character == '\uFEFF'
                || Character.isSpaceChar(character);
    }
}
