package com.daebangg.sayroam.global.mail;

import org.springframework.util.Assert;

import java.net.URI;
import java.util.List;

/** 공통 양식에 넣는 일반 텍스트. HTML은 렌더러에서만 구성한다. */
public record BrandedEmailContent(
        String subject,
        String preheader,
        String category,
        String title,
        String introduction,
        List<String> details,
        Highlight highlight,
        Action action,
        String notice) {

    public BrandedEmailContent {
        Assert.hasText(subject, "Email subject is required");
        Assert.isTrue(!subject.contains("\r") && !subject.contains("\n"), "Email subject must be a single line");
        Assert.hasText(preheader, "Email preheader is required");
        Assert.hasText(category, "Email category is required");
        Assert.hasText(title, "Email title is required");
        Assert.hasText(introduction, "Email introduction is required");
        details = List.copyOf(details);
        details.forEach(detail -> Assert.hasText(detail, "Email detail must not be blank"));
        Assert.hasText(notice, "Email notice is required");
    }

    @Override
    public String toString() {
        return "BrandedEmailContent[content=<redacted>]";
    }

    public record Highlight(String label, String value, String hint) {
        public Highlight {
            Assert.hasText(label, "Highlight label is required");
            Assert.hasText(value, "Highlight value is required");
            Assert.hasText(hint, "Highlight hint is required");
        }

        @Override
        public String toString() {
            return "Highlight[content=<redacted>]";
        }
    }

    public record Action(String label, URI url) {
        public Action {
            Assert.hasText(label, "Action label is required");
            Assert.notNull(url, "Action URL is required");
            Assert.isTrue("https".equalsIgnoreCase(url.getScheme()) && url.getHost() != null
                    && url.getUserInfo() == null, "Action URL must be an absolute HTTPS URL without user info");
        }

        @Override
        public String toString() {
            return "Action[content=<redacted>]";
        }
    }
}
