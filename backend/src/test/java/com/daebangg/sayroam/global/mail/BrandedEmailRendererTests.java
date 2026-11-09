package com.daebangg.sayroam.global.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrandedEmailRendererTests {
    private final BrandedEmailRenderer renderer = new BrandedEmailRenderer();

    @Test
    void escapesEveryDynamicFieldWithoutEvaluatingInsertedPlaceholders() {
        String text = "<script>alert(1)</script> & \"quoted\" {{title}} $1 \\";
        var content = new BrandedEmailContent(EmailLanguage.KO, "안내", text, text, text, text, List.of(text),
                new BrandedEmailContent.Highlight(text, "012345", text),
                new BrandedEmailContent.Action(text, URI.create("https://example.test/trips?a=1&b=2")), text);

        var result = renderer.render(content);

        assertThat(result.html()).doesNotContain("<script>", "alert(1)</script>", "href=\"https://example.test/trips?a=1&b=2\"")
                .contains("&lt;script&gt;", "&amp;", "&quot;quoted&quot;", "{{title}}", "$1", "012345",
                        "href=\"https://example.test/trips?a=1&amp;b=2\"");
        assertThat(result.plainText()).contains(text, "012345", "https://example.test/trips?a=1&b=2");
        assertThat(result.subject()).isEqualTo("[SayRoam] 안내");
        assertThat(result.toString()).doesNotContain("012345", text);
        assertThat(content.toString()).doesNotContain("012345", text);
        assertThat(content.highlight().toString()).doesNotContain("012345");
    }

    @Test
    void supportsTextOnlyContentWithoutAnEmptyCodeOrActionBlock() {
        var content = new BrandedEmailContent(EmailLanguage.KO, "여행 안내", "여행 안내를 확인해 주세요.", "여행 소식",
                "다음 여행을 준비해요", "변경된 일정을 확인해 주세요.", List.of(), null, null, "동행과 함께 확인해 주세요.");

        var result = renderer.render(content);

        assertThat(result.html()).doesNotContain("{{", "인증 코드", "href=").contains("다음 여행을 준비해요", "cid:sayroam-logo", "cid:sayroam-travel");
        assertThat(result.plainText()).contains("변경된 일정을 확인해 주세요.", "동행과 함께 확인해 주세요.");
        assertThat(renderer.inlineAssets()).hasSize(2).allSatisfy(asset -> assertThat(asset.resource().exists()).isTrue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "http://example.test", "/trips", "https://user:password@example.test"})
    void rejectsUnsafeActionDestinations(String url) {
        assertThatThrownBy(() -> new BrandedEmailContent.Action("열기", URI.create(url)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMultilineSubjects() {
        assertThatThrownBy(() -> new BrandedEmailContent(EmailLanguage.KO, "안내\r\nBcc: other@example.test", "안내", "안내", "안내", "안내",
                List.of(), null, null, "안내")).isInstanceOf(IllegalArgumentException.class);
    }
}
