package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.Nickname;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class SocialSignupService {
    private final SocialAuthTicketStore tickets;
    private final AccountLookup accounts;
    private final AccountWriteService accountWrites;
    private final LoginTokenService tokens;

    public SocialSignupService(SocialAuthTicketStore tickets,
                               AccountLookup accounts, AccountWriteService accountWrites, LoginTokenService tokens) {
        this.tickets = tickets;
        this.accounts = accounts;
        this.accountWrites = accountWrites;
        this.tokens = tokens;
    }

    public TokenPairResult signup(String ticket, String browserBinding) {
        if (!OAuthSecrets.isValid(ticket) || !OAuthSecrets.isValid(browserBinding)) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
        }
        SocialIdentity identity = tickets.consumeSignup(VerificationDigests.sha256(ticket), VerificationDigests.sha256(browserBinding))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID));
        return complete(identity);
    }

    public TokenPairResult complete(SocialIdentity identity) {
        return completeWithOutcome(identity).tokens();
    }

    public Completion completeWithOutcome(SocialIdentity identity) {
        if (!identity.emailVerified() || !EmailAddress.isValid(identity.email())) {
            throw new AuthException(AuthErrorCode.SOCIAL_EMAIL_REQUIRED);
        }
        String email = EmailAddress.normalize(identity.email());
        Optional<SocialAccountResult> existing = accounts.findActiveSocialAccountByEmail(email);
        SocialAccountResult account = existing.isPresent() ? accountWrites.linkVerifiedEmail(existing.orElseThrow(), identity)
                : accountWrites.registerSocial(identity, email, Nickname.fromDisplayName(identity.displayName()).value(), Instant.now());
        // 가입 커밋 후 토큰 발급 실패로 회원 생성을 되돌리지 않는다. 새 소셜 로그인으로 확인한다.
        return new Completion(tokens.issue(account.userId(), account.authVersion()), existing.isEmpty());
    }

    public record Completion(TokenPairResult tokens, boolean newlyRegistered) {
        @Override public String toString() { return "Completion[redacted]"; }
    }
}
