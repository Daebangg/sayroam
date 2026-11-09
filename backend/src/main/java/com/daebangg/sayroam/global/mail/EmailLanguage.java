package com.daebangg.sayroam.global.mail;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public enum EmailLanguage {
    KO("ko"), EN("en");

    private final String tag;

    EmailLanguage(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    /** 지원 언어 중 q 우선순위·지역 하위 태그를 고려한다. 메일 요청은 잘못된 헤더 때문에 실패시키지 않는다. */
    public static EmailLanguage fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) return KO;
        try {
            var ranges = Locale.LanguageRange.parse(header).stream().filter(range -> range.getWeight() > 0).toList();
            return "en".equals(Locale.lookupTag(ranges, List.of("ko", "en"))) ? EN : KO;
        } catch (IllegalArgumentException exception) {
            return KO;
        }
    }

    public String text(String key, Object... arguments) {
        var locale = Locale.forLanguageTag(tag);
        var bundle = ResourceBundle.getBundle("mail.messages", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        String pattern = bundle.getString(key);
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, locale).format(arguments);
    }
}
