package com.daebangg.sayroam.auth.infrastructure.smtp;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.infrastructure.config.AccountVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.config.EmailVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.config.PasswordResetProperties;
import com.daebangg.sayroam.global.mail.BrandedEmailContent;
import com.daebangg.sayroam.global.mail.BrandedEmailRenderer;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpVerificationEmailSenderTests {
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final BrandedEmailRenderer renderer = new BrandedEmailRenderer();
    private final VerificationEmailTemplates templates = new VerificationEmailTemplates(
            new EmailVerificationProperties(300, 60, 3600, 5, 20, 5, 600),
            new AccountVerificationProperties(420, 60, 3600, 5, 20, 5, 600),
            new PasswordResetProperties(181, 60, 3600, 5, 20, 5, 300));
    private final SmtpVerificationEmailSender sender = new SmtpVerificationEmailSender(mail, renderer, templates, "no-reply@example.test");

    @Test
    void sendsUtf8AlternativesAndResolvableInlineImagesWithoutExternalImageRequests() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);

        sender.sendVerificationCode("member@example.test", "012345");

        verify(mail).send(message);
        // 실제 전송 직전의 바이트를 다시 읽어 MIME 구조와 한글·선행 0 보존을 확인한다.
        var output = new ByteArrayOutputStream();
        message.writeTo(output);
        var delivered = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(output.toByteArray()));
        assertThat(delivered.getSubject()).isEqualTo("[SayRoam] 이메일 인증 코드");
        assertThat(delivered.getFrom()[0].toString()).isEqualTo("no-reply@example.test");
        assertThat(delivered.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("member@example.test");
        assertThat(delivered.isMimeType("multipart/related")).isTrue();
        Multipart related = (Multipart) delivered.getContent();
        assertThat(related.getCount()).isEqualTo(3);
        Multipart alternatives = (Multipart) related.getBodyPart(0).getContent();
        assertThat(alternatives.getCount()).isEqualTo(2);
        assertThat(alternatives.getBodyPart(0).isMimeType("text/plain")).isTrue();
        assertThat(alternatives.getBodyPart(0).getContent().toString()).contains("인증 코드: 012345", "최대 5분", "본인이 요청하지 않았다면");
        BodyPart html = alternatives.getBodyPart(1);
        assertThat(html.isMimeType("text/html")).isTrue();
        assertThat(html.getContent().toString()).contains("012345", "cid:sayroam-logo", "cid:sayroam-travel")
                .doesNotContain("src=\"http", "{{");
        assertImage(related.getBodyPart(1), "<sayroam-logo>", "image/png");
        assertImage(related.getBodyPart(2), "<sayroam-travel>", "image/jpeg");
    }

    @Test
    void keepsEachVerificationPurposeAndItsConfiguredLifetimeDistinct() {
        assertContent(templates.signup("012345"), "이메일 인증", "최대 5분", "회원가입 이메일 인증에만");
        assertContent(templates.account("012345", AccountVerificationPurpose.PASSWORD_CHANGE), "비밀번호 변경", "최대 7분", "비밀번호 변경 본인 확인에만");
        assertContent(templates.account("012345", AccountVerificationPurpose.ACCOUNT_DELETION), "회원 탈퇴", "최대 7분", "코드 확인만으로 탈퇴가 완료되지는");
        var reset = renderer.render(templates.passwordReset("012345"));
        assertThat(reset.plainText()).contains("최대 181초", "기존 이메일·비밀번호 로그인 계정에만", "소셜 로그인으로만 가입했다면");
        assertThat(reset.html()).contains("기존 이메일·비밀번호 로그인 계정에만", "소셜 로그인으로만 가입했다면");
    }

    @Test
    void preservesDeliveryFailureMappingWithoutExposingSmtpDetails() {
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        var failure = new MailSendException("private SMTP diagnostic");
        doThrow(failure).when(mail).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.sendPasswordResetCode("member@example.test", "012345"))
                .isInstanceOfSatisfying(AuthException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_DELIVERY_FAILED);
                    assertThat(exception.getCause()).isSameAs(failure);
                    assertThat(exception.getMessage()).doesNotContain("private SMTP diagnostic", "member@example.test", "012345");
                });
    }

    private void assertContent(BrandedEmailContent content, String subject, String duration, String notice) {
        var result = renderer.render(content);
        assertThat(result.subject()).contains(subject);
        assertThat(result.plainText()).contains("012345", duration, notice);
        assertThat(result.html()).contains("012345", duration, notice);
    }

    private void assertImage(BodyPart part, String cid, String type) throws Exception {
        assertThat(part.getHeader("Content-ID")).containsExactly(cid);
        assertThat(part.isMimeType(type)).isTrue();
        assertThat(part.getDisposition()).isEqualTo("inline");
        assertThat(part.getInputStream().readAllBytes()).isNotEmpty();
    }
}
