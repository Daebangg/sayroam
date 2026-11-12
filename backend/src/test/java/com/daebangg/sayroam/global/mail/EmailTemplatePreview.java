package com.daebangg.sayroam.global.mail;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.infrastructure.persistence.AccountVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.persistence.EmailVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.persistence.PasswordResetProperties;
import com.daebangg.sayroam.auth.infrastructure.messaging.VerificationEmailTemplates;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SMTP·Spring 실행 없이 합성 코드 012345만 사용하는 로컬 검토 도구. */
public class EmailTemplatePreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output.resolve("assets"));
        BrandedEmailRenderer renderer = new BrandedEmailRenderer();
        VerificationEmailTemplates templates = new VerificationEmailTemplates(
                new EmailVerificationProperties(300, 60, 3600, 5, 20, 5, 600),
                new AccountVerificationProperties(300, 60, 3600, 5, 20, 5, 600),
                new PasswordResetProperties(300, 60, 3600, 5, 20, 5, 300));
        LinkedHashMap<String, BrandedEmailContent> samples = new LinkedHashMap<String, BrandedEmailContent>();
        samples.put("signup", templates.signup("012345", EmailLanguage.KO));
        samples.put("password-reset", templates.passwordReset("012345", EmailLanguage.KO));
        samples.put("password-change", templates.account("012345", AccountVerificationPurpose.PASSWORD_CHANGE, EmailLanguage.KO));
        samples.put("account-deletion", templates.account("012345", AccountVerificationPurpose.ACCOUNT_DELETION, EmailLanguage.KO));
        samples.put("travel-notice", new BrandedEmailContent(EmailLanguage.KO, "여행 준비 안내", "다음 여행을 함께 준비해요.", "함께 준비하는 여행",
                "우리의 다음 여행이 기다려요", "일정과 가고 싶은 장소를 모아 하나의 여행 계획으로 만들어 보세요.",
                List.of("같은 취향의 동행과 여행을 준비하면 더 특별한 순간을 만날 수 있어요."), null,
                new BrandedEmailContent.Action("여행 계획 확인하기", URI.create("https://example.test/trips")),
                "이 화면은 디자인 검토용 예시이며 실제 여행이나 발송 요청을 만들지 않아요."));
        samples.put("signup-en", templates.signup("012345", EmailLanguage.EN));
        samples.put("password-reset-en", templates.passwordReset("012345", EmailLanguage.EN));
        samples.put("password-change-en", templates.account("012345", AccountVerificationPurpose.PASSWORD_CHANGE, EmailLanguage.EN));
        samples.put("account-deletion-en", templates.account("012345", AccountVerificationPurpose.ACCOUNT_DELETION, EmailLanguage.EN));
        samples.put("travel-notice-en", new BrandedEmailContent(EmailLanguage.EN, "Your next journey", "Get ready for your next journey.",
                "PREPARE TOGETHER", "Your next journey is waiting", "Bring your itinerary and favorite places together in one travel plan.",
                List.of("Find travel companions who share your interests and prepare for a memorable journey together."), null,
                new BrandedEmailContent.Action("View travel plan", URI.create("https://example.test/trips")),
                "This is a design preview. It does not create a real trip or send an email."));
        for (BrandedEmailRenderer.InlineAsset asset : renderer.inlineAssets()) {
            try (InputStream stream = asset.resource().getInputStream()) {
                Files.copy(stream, output.resolve("assets/" + asset.resource().getFilename()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        StringBuilder links = new StringBuilder();
        for (Map.Entry<String, BrandedEmailContent> sample : samples.entrySet()) {
            RenderedEmail rendered = renderer.render(sample.getValue());
            String html = rendered.html();
            for (BrandedEmailRenderer.InlineAsset asset : renderer.inlineAssets()) {
                html = html.replace("cid:" + asset.contentId(), "assets/" + asset.resource().getFilename());
            }
            Files.writeString(output.resolve(sample.getKey() + ".html"), html, StandardCharsets.UTF_8);
            Files.writeString(output.resolve(sample.getKey() + ".txt"), rendered.plainText(), StandardCharsets.UTF_8);
            links.append("<li><a href=\"").append(sample.getKey()).append(".html\">")
                    .append(rendered.subject()).append("</a> · <a href=\"").append(sample.getKey()).append(".txt\">일반 텍스트</a></li>\n");
        }
        Files.writeString(output.resolve("index.html"), """
                <!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>SayRoam 이메일 미리보기</title><body style="font-family:sans-serif;padding:24px;background:#edf2fc;color:#17396d;">
                <h1>SayRoam 이메일 미리보기</h1><p>모든 코드·링크는 검토용 예시입니다. 메일을 발송하지 않습니다.</p><ul>
                """ + links + "</ul></body></html>", StandardCharsets.UTF_8);
        System.out.println("Email previews: " + output.resolve("index.html").toAbsolutePath());
    }
}
