package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.PasswordChange;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import org.springframework.stereotype.Service;

@Service
public class PasswordChangeService {
    private final AccountLookup accounts;
    private final PasswordHasher passwords;
    private final AccountVerificationService verification;
    private final PasswordChange passwordChange;

    public PasswordChangeService(AccountLookup accounts, PasswordHasher passwords,
                                 AccountVerificationService verification, PasswordChange passwordChange) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.verification = verification;
        this.passwordChange = passwordChange;
    }

    public void change(long userId, long expectedAuthVersion, String accountProof, String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) {
            throw new AuthException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
        }
        AccountLookup.VerifiedAccount account = accounts.findActiveVerifiedAccount(userId).orElseThrow(
                () -> new AuthException(AuthErrorCode.ACCOUNT_VERIFICATION_NOT_ALLOWED));
        if (account.authVersion() != expectedAuthVersion) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
        }
        AccountLookup.Credentials credentials = accounts.findActiveCredentials(account.email()).filter(value -> value.userId() == userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.PASSWORD_CHANGE_NOT_ALLOWED));
        if (credentials.authVersion() != expectedAuthVersion) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
        }
        // 해시 생성 실패로 유효한 증명을 잃지 않도록 소비 전에 처리한다.
        String hash = passwords.hash(newPassword);
        // 작업 목적은 요청에서 받지 않는다. 가입·탈퇴 증명으로 비밀번호를 변경할 수 없다.
        AccountVerificationService.VerifiedAction verified = verification.consumeProof(userId, expectedAuthVersion,
                AccountVerificationPurpose.PASSWORD_CHANGE, accountProof);
        passwordChange.change(userId, verified.email(), verified.authVersion(), hash);
        // 이후 Redis 정리 호출을 하지 않는다. DB 커밋 자체가 모든 이전 버전을 차단한다.
    }
}
