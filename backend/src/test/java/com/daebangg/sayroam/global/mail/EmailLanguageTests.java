package com.daebangg.sayroam.global.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class EmailLanguageTests {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "en-US,en;q=0.9,ko;q=0.8|EN", "ko-KR,ko;q=0.9,en;q=0.8|KO",
            "ko;q=0.2,en;q=0.9|EN", "fr-FR,en-GB;q=0.8|EN", "en;q=0,ko;q=0.5|KO",
            "EN-us|EN", "en;q=0|KO", "*|KO", "ja-JP|KO", "en;q=oops|KO"
    })
    void selectsSupportedLanguageByRegionAndQuality(String header, EmailLanguage expected) {
        assertThat(EmailLanguage.fromAcceptLanguage(header)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void defaultsToKoreanWhenNoPreferenceIsSent(String header) {
        assertThat(EmailLanguage.fromAcceptLanguage(header)).isEqualTo(EmailLanguage.KO);
    }

    @Test
    void hasCompleteTranslationsAndFormatsSingleAndPluralDurations() {
        ResourceBundle korean = ResourceBundle.getBundle("mail.messages", Locale.KOREAN);
        ResourceBundle english = ResourceBundle.getBundle("mail.messages", Locale.ENGLISH);
        assertThat(english.keySet()).containsExactlyInAnyOrderElementsOf(korean.keySet());
        assertThat(EmailLanguage.EN.text("duration.minutes", 1)).isEqualTo("1 minute");
        assertThat(EmailLanguage.EN.text("duration.minutes", 5)).isEqualTo("5 minutes");
        assertThat(EmailLanguage.EN.text("duration.seconds", 181)).isEqualTo("181 seconds");
    }
}
