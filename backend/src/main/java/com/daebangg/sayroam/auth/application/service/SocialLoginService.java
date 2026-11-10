package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialAuthPolicy;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.port.out.OAuthTransactionStore;
import com.daebangg.sayroam.auth.application.port.out.SocialAccounts;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class SocialLoginService {
    private final SocialIdentityProvider identities;
    private final OAuthTransactionStore transactions;
    private final SocialAuthTicketStore tickets;
    private final SocialAccounts accounts;
    private final LoginTokenService tokens;
    private final SocialAuthPolicy policy;

    public java.util.List<SocialProvider> availableProviders() {
        return identities.availableProviders();
    }

    public SocialLoginService(SocialIdentityProvider identities, OAuthTransactionStore transactions,
                              SocialAuthTicketStore tickets, SocialAccounts accounts,
                              LoginTokenService tokens, SocialAuthPolicy policy) {
        this.identities = identities;
        this.transactions = transactions;
        this.tickets = tickets;
        this.accounts = accounts;
        this.tokens = tokens;
        this.policy = policy;
    }

    public Started start(SocialProvider provider, String returnPath, String browserBinding) {
        if (!policy.allowedReturnPaths().contains(returnPath)) {
            throw new AuthException(AuthErrorCode.SOCIAL_RETURN_PATH_INVALID);
        }
        requireSecret(browserBinding);
        String state = OAuthSecrets.create();
        String verifier = OAuthSecrets.create();
        var transaction = new OAuthTransactionStore.Transaction(provider, OAuthTransactionStore.Purpose.LOGIN,
                digest(browserBinding), returnPath, verifier, null);
        var uri = identities.authorizationUri(provider, new SocialIdentityProvider.AuthorizationRequest(state,
                OAuthSecrets.challenge(verifier)));
        if (!transactions.save(digest(state), transaction, policy.transactionTtl())) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
        return new Started(uri, policy.transactionTtl().toSeconds());
    }

    public URI callback(SocialProvider provider, String state, String code, String error, String browserBinding) {
        requireSecret(state);
        requireSecret(browserBinding);
        var transaction = transactions.consume(digest(state), provider, digest(browserBinding))
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
            var identity = identities.authenticate(provider, new SocialIdentityProvider.Callback(code, state,
                    transaction.codeVerifier()));
            if (identity.provider() != provider) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            var account = accounts.findActive(provider, identity.providerUserId());
            if (account.isPresent()) {
                result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.LOGIN_READY, identity, account.orElseThrow(), null);
            } else {
                if (identity.emailVerified() && accounts.emailExists(EmailAddress.normalize(identity.email()))) {
                    throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
                }
                result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.SIGNUP_REQUIRED, identity, null, null);
            }
        } catch (AuthException exception) {
            result = new SocialAuthTicketStore.Result(SocialAuthTicketStore.Status.FAILED, null, null, exception.getErrorCode());
        }
        String resultCode = OAuthSecrets.create();
        if (!tickets.saveResult(digest(resultCode), digest(browserBinding), result, policy.resultTtl())) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
        // URL에는 로그인 토큰·회원 정보·공급자 오류를 넣지 않는다. fragment는 HTTP Referer로 전송되지 않는다.
        URI location = URI.create(transaction.returnPath() + "#social_result=" + resultCode);
        return policy.frontendOrigin() == null ? location : policy.frontendOrigin().resolve(location);
    }

    public Exchanged exchange(String resultCode, String browserBinding) {
        requireSecret(resultCode);
        requireSecret(browserBinding);
        var result = tickets.consumeResult(digest(resultCode), digest(browserBinding))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID));
        return switch (result.status()) {
            case FAILED -> throw new AuthException(result.error());
            case LOGIN_READY -> {
                // 콜백 이후 탈퇴·연결 해제·인증 버전 변경도 다시 검사한다.
                var current = accounts.findActive(result.identity().provider(), result.identity().providerUserId())
                        .filter(account -> account.equals(result.account()))
                        .orElseThrow(() -> new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED));
                yield new Exchanged(tokens.issue(current.userId(), current.authVersion()), null);
            }
            case SIGNUP_REQUIRED -> {
                String ticket = OAuthSecrets.create();
                if (!tickets.saveSignup(digest(ticket), digest(browserBinding), result.identity(), policy.signupTtl())) {
                    throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
                }
                yield new Exchanged(null, new PendingSignup(ticket, policy.signupTtl().toSeconds(),
                        !result.identity().emailVerified(), result.identity().email()));
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
    public record Exchanged(TokenPair tokens, PendingSignup signup) {
        @Override public String toString() { return "Exchanged[redacted]"; }
    }
}
