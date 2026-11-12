package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.OAuthTransactionStore;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.SocialAuthSettings;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.SocialProvider;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SocialLoginService {
    private final SocialIdentityProvider identities;
    private final OAuthTransactionStore transactions;
    private final SocialAuthTicketStore tickets;
    private final AccountLookup accounts;
    private final AccountWriteService accountWrites;
    private final LoginTokenService tokens;
    private final SocialAuthSettings settings;
    private final SocialSignupService signup;

    public SocialLoginService(SocialIdentityProvider identities, OAuthTransactionStore transactions,
                              SocialAuthTicketStore tickets, AccountLookup accounts, AccountWriteService accountWrites,
                              LoginTokenService tokens, SocialAuthSettings settings, SocialSignupService signup) {
        this.identities = identities;
        this.transactions = transactions;
        this.tickets = tickets;
        this.accounts = accounts;
        this.accountWrites = accountWrites;
        this.tokens = tokens;
        this.settings = settings;
        this.signup = signup;
    }

    public List<SocialProvider> availableProviders() {
        return identities.availableProviders();
    }

    public SocialProvider parseProvider(String value) {
        try {
            return SocialProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_DISABLED);
        }
    }

    public Started start(SocialProvider provider, String returnPath, String browserBinding, boolean requestEmailConsent) {
        settings.requireReturnPath(returnPath);
        requireSecret(browserBinding);
        String state = OAuthSecrets.create();
        String verifier = OAuthSecrets.create();
        OAuthTransactionStore.Transaction transaction = new OAuthTransactionStore.Transaction(provider, OAuthTransactionStore.Purpose.LOGIN,
                digest(browserBinding), returnPath, verifier, null);
        URI uri = identities.authorizationUri(provider, new SocialIdentityProvider.AuthorizationRequest(state,
                OAuthSecrets.challenge(verifier), requestEmailConsent));
        if (!transactions.save(digest(state), transaction, settings.transactionTtl())) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
        return new Started(uri, settings.transactionTtl().toSeconds());
    }

    public URI callback(SocialProvider provider, String state, String code, String error, String browserBinding) {
        requireSecret(state);
        requireSecret(browserBinding);
        OAuthTransactionStore.Transaction transaction = transactions.consume(digest(state), provider, digest(browserBinding))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID));
        if (transaction.purpose() != OAuthTransactionStore.Purpose.LOGIN) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
        }
        SocialAuthTicketStore.Result result;
        try {
            if (error != null) {
                throw new AuthException("access_denied".equals(error) && code == null
                        ? AuthErrorCode.SOCIAL_AUTH_CANCELLED : AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            if (code == null || code.isBlank() || code.length() > 4096) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            SocialIdentity identity = identities.authenticate(provider, new SocialIdentityProvider.Callback(code, state,
                    transaction.codeVerifier()));
            if (identity.provider() != provider) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            Optional<SocialAccountResult> account = accounts.findActiveSocialAccount(provider, identity.providerUserId());
            if (account.isPresent()) {
                result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.LOGIN_READY, identity, account.orElseThrow(), null);
            } else {
                if (!identity.emailVerified() || !EmailAddress.isValid(identity.email())) {
                    throw new AuthException(AuthErrorCode.SOCIAL_EMAIL_REQUIRED);
                }
                String email = EmailAddress.normalize(identity.email());
                Optional<SocialAccountResult> existing = accounts.findActiveSocialAccountByEmail(email);
                if (existing.isPresent()) {
                    result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.AUTO_LINK_READY, identity, existing.orElseThrow(), null);
                } else if (socialEmailExists(email)) {
                    throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
                } else {
                    result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.SIGNUP_REQUIRED, identity, null, null);
                }
            }
        } catch (AuthException exception) {
            result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.FAILED, null, null, exception.getErrorCode());
        }
        String resultCode = OAuthSecrets.create();
        if (!tickets.saveResult(digest(resultCode), digest(browserBinding), result, settings.resultTtl())) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
        // URL에는 로그인 토큰·회원 정보·공급자 오류를 넣지 않는다. fragment는 HTTP Referer로 전송되지 않는다.
        URI location = URI.create(transaction.returnPath() + "#social_result=" + resultCode);
        return settings.frontendOrigin() == null ? location : settings.frontendOrigin().resolve(location);
    }

    private boolean socialEmailExists(String email) {
        try {
            return accounts.emailExists(email);
        } catch (AuthException exception) {
            // 공통 이메일 조회가 실패해도 소셜 콜백은 기존 소셜 오류 코드로 결과를 저장한다.
            if (exception.getErrorCode() == AuthErrorCode.AUTHENTICATION_UNAVAILABLE) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, exception);
            }
            throw exception;
        }
    }

    public Exchanged exchange(String resultCode, String browserBinding) {
        requireSecret(resultCode);
        requireSecret(browserBinding);
        SocialAuthTicketStore.Result result = tickets.consumeResult(digest(resultCode), digest(browserBinding))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID));
        return switch (result.status()) {
            case FAILED -> throw new AuthException(result.error());
            case AUTO_LINK_READY -> {
                SocialAccountResult current = accountWrites.linkVerifiedEmail(result.account(), result.identity());
                yield new Exchanged(tokens.issue(current.userId(), current.authVersion()), null, false);
            }
            case LOGIN_READY -> {
                // 콜백 이후 탈퇴·연결 해제·인증 버전 변경도 다시 검사한다.
                SocialAccountResult current = accounts.findActiveSocialAccount(result.identity().provider(), result.identity().providerUserId())
                        .filter(account -> account.equals(result.account()))
                        .orElseThrow(() -> new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED));
                yield new Exchanged(tokens.issue(current.userId(), current.authVersion()), null, false);
            }
            case SIGNUP_REQUIRED -> {
                // 신규 회원도 공급자 이름으로 가입한 뒤 같은 응답에서 로그인한다.
                SocialSignupService.Completion completed = signup.completeWithOutcome(result.identity());
                yield new Exchanged(completed.tokens(), null, completed.newlyRegistered());
            }
        };
    }

    private static void requireSecret(String value) {
        if (!OAuthSecrets.isValid(value)) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
    }

    private static String digest(String value) { return VerificationDigests.sha256(value); }

    public record Started(URI authorizationUrl, long expiresInSeconds) {
        @Override public String toString() { return "Started[redacted]"; }
    }
    public record PendingSignup(String signupTicket, long expiresInSeconds, boolean emailVerificationRequired, String suggestedEmail) {
        @Override public String toString() { return "PendingSignup[redacted]"; }
    }
    public record Exchanged(TokenPairResult tokens, PendingSignup signup, boolean newlyRegistered) {
        @Override public String toString() { return "Exchanged[redacted]"; }
    }
}
