package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.global.mail.EmailLanguage;

public interface VerificationEmailSender {
    /** @throws com.daebangg.sayroam.auth.domain.error.AuthException 메일 전송 기술 오류가 발생한 경우 */
    void sendVerificationCode(String recipient, String code, EmailLanguage language);

    void sendAccountVerificationCode(String recipient, String code, AccountVerificationPurpose purpose, EmailLanguage language);

    void sendPasswordResetCode(String recipient, String code, EmailLanguage language);
}
