package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import org.springframework.stereotype.Service;

@Service
public class AccountDeletionService {
    private final AccountVerificationService verification;
    private final AccountWriteService accountDeletion;

    public AccountDeletionService(AccountVerificationService verification, AccountWriteService accountDeletion) {
        this.verification = verification;
        this.accountDeletion = accountDeletion;
    }

    public void delete(long userId, long expectedAuthVersion, String accountProof) {
        // 비밀번호 등록 여부와 무관하다. 소셜 계정도 현재 인증된 대표 이메일로 본인 확인해야 한다.
        AccountVerificationService.VerifiedAction verified = verification.consumeProof(userId, expectedAuthVersion,
                AccountVerificationPurpose.ACCOUNT_DELETION, accountProof);
        accountDeletion.delete(userId, verified.email(), verified.authVersion());
        // DB 커밋이 로그인 차단의 기준이다. 커밋 뒤 Redis/SMTP 작업으로 성공 여부를 바꾸지 않는다.
    }
}
