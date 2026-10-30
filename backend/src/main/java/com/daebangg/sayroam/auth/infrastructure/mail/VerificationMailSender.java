package com.daebangg.sayroam.auth.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class VerificationMailSender {
    private final JavaMailSender mailSender;
    private final String from;

    // 메일을 활성화할 때만 이 컴포넌트와 MailConfig의 JavaMailSender가 함께 등록된다.
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public VerificationMailSender(JavaMailSender mailSender, @Value("${app.mail.from:}") String from) {
        Assert.hasText(from, "app.mail.from must be configured when SMTP is enabled");
        this.mailSender = mailSender;
        this.from = from;
    }

    // 코드 생성·보관·검증은 호출 측 책임이다. SMTP 실패는 MailException으로 전파한다.
    public void sendVerificationCode(String recipient, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("[Sayroam] 이메일 인증 코드");
        message.setText("이메일 인증 코드: " + code + "\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
        mailSender.send(message);
    }
}
