package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountRegistration;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

@Service
public class SignupService {
    private final EmailVerificationStore verificationStore;
    private final PasswordHasher passwordHasher;
    private final AccountRegistration accounts;

    public SignupService(EmailVerificationStore verificationStore, PasswordHasher passwordHasher,
                         AccountRegistration accounts) {
        this.verificationStore = verificationStore;
        this.passwordHasher = passwordHasher;
        this.accounts = accounts;
    }

    public long signup(String rawEmail, String password, String nickname, String signupProof) {
        String email = EmailAddress.normalize(rawEmail);
        var proof = verificationStore.consumeSignupProof(email, VerificationDigests.sha256(signupProof));
        switch (proof.status()) {
            case EXPIRED -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_EXPIRED);
            case INVALID -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_INVALID);
            case ALREADY_USED -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_ALREADY_USED);
            case CONSUMED -> { }
        }
        // 증명 소비 후 실패하더라도 되살리지 않는다. 재가입 시 새 이메일 인증이 필요하다.
        // DB 커밋 후에는 Redis를 호출하지 않아 정리 실패가 가입 성공을 뒤집지 않도록 한다.
        String passwordHash = passwordHasher.hash(password);
        return accounts.register(email, nickname, passwordHash, proof.verifiedAt());
    }
}
