package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.SocialAccounts;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SocialSignupService {
    private final SocialAuthTicketStore tickets;
    private final EmailVerificationStore emails;
    private final SocialAccounts accounts;
    private final LoginTokenService tokens;

    public SocialSignupService(SocialAuthTicketStore tickets, EmailVerificationStore emails,
                               SocialAccounts accounts, LoginTokenService tokens) {
        this.tickets = tickets;
        this.emails = emails;
        this.accounts = accounts;
        this.tokens = tokens;
    }

    public TokenPair signup(String ticket, String browserBinding, String nickname, String rawEmail, String proof) {
        if (!OAuthSecrets.isValid(ticket) || !OAuthSecrets.isValid(browserBinding)) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
        }
        var identity = tickets.consumeSignup(VerificationDigests.sha256(ticket), VerificationDigests.sha256(browserBinding))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID));
        String email;
        Instant verifiedAt;
        if (identity.emailVerified()) {
            email = EmailAddress.normalize(identity.email());
            if (rawEmail != null && !EmailAddress.normalize(rawEmail).equals(email)) {
                throw new AuthException(AuthErrorCode.SOCIAL_SIGNUP_INVALID);
            }
            verifiedAt = Instant.now();
        } else {
            if (rawEmail == null || rawEmail.isBlank() || !OAuthSecrets.isValid(proof)) {
                throw new AuthException(AuthErrorCode.SOCIAL_SIGNUP_INVALID);
            }
            email = EmailAddress.normalize(rawEmail);
            var consumed = emails.consumeSignupProof(email, VerificationDigests.sha256(proof));
            switch (consumed.status()) {
                case EXPIRED -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_EXPIRED);
                case INVALID -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_INVALID);
                case ALREADY_USED -> throw new AuthException(AuthErrorCode.SIGNUP_PROOF_ALREADY_USED);
                case CONSUMED -> { }
            }
            verifiedAt = consumed.verifiedAt();
        }
        var account = accounts.register(identity, email, nickname, verifiedAt);
        // 가입 커밋 후 토큰 발급 실패로 회원 생성을 되돌리지 않는다. 새 소셜 로그인으로 확인한다.
        return tokens.issue(account.userId(), account.authVersion());
    }
}
