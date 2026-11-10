package com.daebangg.sayroam.global.validation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class InputTextTests {
    @Test
    void trimsJavaScriptWhitespaceIncludingUnicodeSeparatorsAndBom() {
        String whitespace = "\t\n\u000B\f\r \u00a0\u1680\u2000\u2001\u2002\u2003\u2004\u2005"
                + "\u2006\u2007\u2008\u2009\u200a\u2028\u2029\u202f\u205f\u3000\uFEFF";
        assertThat(InputText.trim(null)).isNull();
        assertThat(InputText.trim("")).isEmpty();
        assertThat(InputText.trim(whitespace)).isEmpty();
        assertThat(InputText.trim(whitespace + "여행자😀" + whitespace)).isEqualTo("여행자😀");
    }

    @Test
    void preservesInternalWhitespaceAndNonTrimmableBoundaryCharacters() {
        for (String value : List.of("a \t\u00a0\u2028\uFEFF b", "\u0085name\u0085", "\u180ename\u180e",
                "\u200bname\u200b", "\u0000name\u0000", "😀name😀")) {
            assertThat(InputText.trim(" " + value + " ")).isEqualTo(value);
        }
    }

    @Test
    void handlesLongInternalAndBoundaryWhitespaceWithoutRepeatedScanning() {
        String whitespace = " ".repeat(100_000);
        String value = "a" + whitespace + "b";
        assertTimeout(Duration.ofSeconds(1), () -> {
            assertThat(InputText.trim(value)).isEqualTo(value);
            assertThat(InputText.trim(whitespace + "user@example.test" + whitespace)).isEqualTo("user@example.test");
        });
    }
}
