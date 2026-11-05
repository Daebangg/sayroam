package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;

public interface VerificationEmailSender {
    /** @throws com.daebangg.sayroam.auth.application.error.AuthException 메일 전송 기술 오류가 발생한 경우 */
    void sendVerificationCode(String recipient, String code);

    void sendAccountVerificationCode(String recipient, String code, AccountVerificationPurpose purpose);
}
