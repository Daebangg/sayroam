package com.daebangg.sayroam.auth.infrastructure.smtp;

import com.daebangg.sayroam.global.mail.EmailLanguage;
import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.global.mail.BrandedEmailContent;
import com.daebangg.sayroam.global.mail.BrandedEmailRenderer;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class SmtpVerificationEmailSender implements VerificationEmailSender {
    private final JavaMailSender mailSender;
    private final BrandedEmailRenderer renderer;
    private final VerificationEmailTemplates templates;
    private final String from;

    // 메일을 활성화할 때만 이 컴포넌트와 MailConfig의 JavaMailSender가 함께 등록된다.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SmtpVerificationEmailSender(JavaMailSender mailSender, BrandedEmailRenderer renderer,
                                       VerificationEmailTemplates templates, @Value("${app.mail.from:}") String from) {
        Assert.hasText(from, "app.mail.from must be configured when SMTP is enabled");
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.templates = templates;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String recipient, String code, EmailLanguage language) {
        send(recipient, templates.signup(code, language));
    }

    @Override
    public void sendAccountVerificationCode(String recipient, String code, AccountVerificationPurpose purpose, EmailLanguage language) {
        send(recipient, templates.account(code, purpose, language));
    }

    @Override
    public void sendPasswordResetCode(String recipient, String code, EmailLanguage language) {
        send(recipient, templates.passwordReset(code, language));
    }

    private void send(String recipient, BrandedEmailContent content) {
        var rendered = renderer.render(content);
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(rendered.subject());
            message.setHeader("Content-Language", content.language().tag());
            helper.setText(rendered.plainText(), rendered.html());
            for (var asset : renderer.inlineAssets()) {
                helper.addInline(asset.contentId(), asset.resource(), asset.contentType());
            }
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            // SMTP 응답·이메일·인증 코드·접속 비밀값은 클라이언트나 로그로 전달하지 않는다.
            log.warn("Verification email delivery failed: {}", exception.getClass().getSimpleName());
            throw new AuthException(AuthErrorCode.EMAIL_DELIVERY_FAILED, exception);
        }
    }
}
