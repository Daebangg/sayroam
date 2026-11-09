package com.daebangg.sayroam.global.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class BrandedEmailRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-z]+)}}");
    private static final List<InlineAsset> ASSETS = List.of(
            new InlineAsset("sayroam-logo", new ClassPathResource("mail/assets/sayroam-logo.png"), "image/png"),
            new InlineAsset("sayroam-travel", new ClassPathResource("mail/assets/travel-coast.jpg"), "image/jpeg"));
    private final String template;

    public BrandedEmailRenderer() {
        try {
            template = new ClassPathResource("mail/base.html").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load the branded email template", exception);
        }
    }

    public RenderedEmail render(BrandedEmailContent content) {
        Map<String, String> values = Map.of(
                "preheader", escape(content.preheader()),
                "category", escape(content.category()),
                "title", escape(content.title()),
                "introduction", escape(content.introduction()),
                "details", content.details().stream().map(this::paragraph).collect(Collectors.joining()),
                "highlight", highlight(content.highlight()),
                "action", action(content.action()),
                "notice", escape(content.notice()));
        // 한 번만 치환해 사용자 문구의 {{title}} 같은 문자열을 다시 평가하지 않는다.
        String html = PLACEHOLDER.matcher(template).replaceAll(match -> {
            String value = values.get(match.group(1));
            if (value == null) {
                throw new IllegalStateException("Unknown email template placeholder: " + match.group(1));
            }
            return Matcher.quoteReplacement(value);
        });
        return new RenderedEmail("[SayRoam] " + content.subject(), plainText(content), html);
    }

    public List<InlineAsset> inlineAssets() {
        return ASSETS;
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 14px;color:#526584;font-size:14px;line-height:1.8;\">"
                + escape(text) + "</p>";
    }

    private String highlight(BrandedEmailContent.Highlight highlight) {
        if (highlight == null) return "";
        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin:8px 0 24px;background:#edf3ff;border-radius:16px;">
                  <tr><td style="padding:24px;text-align:center;">
                    <p style="margin:0 0 12px;color:#385caa;font-size:12px;font-weight:700;">%s</p>
                    <p style="margin:0;color:#17396d;font-family:Arial,sans-serif;font-size:34px;font-weight:700;letter-spacing:7px;line-height:1.4;overflow-wrap:anywhere;">%s</p>
                    <p style="margin:14px 0 0;color:#526584;font-size:12px;line-height:1.7;">%s</p>
                  </td></tr>
                </table>
                """.formatted(escape(highlight.label()), escape(highlight.value()), escape(highlight.hint()));
    }

    private String action(BrandedEmailContent.Action action) {
        if (action == null) return "";
        String url = escape(action.url().toASCIIString());
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:8px 0 18px;">
                  <tr><td bgcolor="#365de8" style="border-radius:24px;text-align:center;mso-padding-alt:13px 24px;">
                    <a href="%s" style="display:inline-block;padding:13px 24px;color:#ffffff;font-size:14px;font-weight:700;text-decoration:none;line-height:1.5;">%s</a>
                  </td></tr>
                </table>
                <p style="margin:0 0 24px;color:#526584;font-size:12px;line-height:1.7;overflow-wrap:anywhere;word-break:break-all;">버튼이 열리지 않으면 아래 주소를 복사해 주세요.<br><a href="%s" style="color:#365de8;">%s</a></p>
                """.formatted(url, escape(action.label()), url, url);
    }

    private String plainText(BrandedEmailContent content) {
        StringBuilder text = new StringBuilder("SayRoam\n\n")
                .append(content.title()).append("\n\n")
                .append(content.introduction()).append("\n\n");
        content.details().forEach(detail -> text.append(detail).append("\n\n"));
        if (content.highlight() != null) {
            var highlight = content.highlight();
            text.append(highlight.label()).append(": ").append(highlight.value()).append("\n")
                    .append(highlight.hint()).append("\n\n");
        }
        if (content.action() != null) {
            text.append(content.action().label()).append("\n").append(content.action().url()).append("\n\n");
        }
        return text.append(content.notice()).append("\n\n")
                .append("함께 떠나는 더 특별한 여행, SayRoam\n이 메일은 SayRoam에서 자동으로 보냈습니다.\n")
                .toString();
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }

    public record InlineAsset(String contentId, Resource resource, String contentType) { }
}
