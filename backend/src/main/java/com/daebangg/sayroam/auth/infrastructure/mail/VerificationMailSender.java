package com.daebangg.sayroam.auth.infrastructure.mail;

import com.daebangg.sayroam.auth.application.AuthErrorCode;
import com.daebangg.sayroam.auth.application.EmailVerificationException;
import com.daebangg.sayroam.auth.application.VerificationEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class VerificationMailSender implements VerificationEmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    // 메일을 활성화할 때만 이 컴포넌트와 MailConfig의 JavaMailSender가 함께 등록된다.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public VerificationMailSender(JavaMailSender mailSender, @Value("${app.mail.from:}") String from) {
        Assert.hasText(from, "app.mail.from must be configured when SMTP is enabled");
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String recipient, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("[Sayroam] 이메일 인증 코드");
        message.setText("이메일 인증 코드: " + code + "\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // SMTP 응답·이메일·인증 코드·접속 비밀값은 클라이언트나 로그로 전달하지 않는다.
            log.warn("Verification email delivery failed: {}", exception.getClass().getSimpleName());
            throw new EmailVerificationException(AuthErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }
}
