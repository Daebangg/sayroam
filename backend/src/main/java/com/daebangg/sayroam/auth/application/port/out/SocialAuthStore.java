package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.user.domain.SocialProvider;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** 소셜 인증의 요청 상태·결과·이전 가입 티켓을 각각 검증하고 한 번만 소비한다. */
public interface SocialAuthStore {
    /** state의 SHA-256을 키로 사용한다. 기존 상태를 덮거나 TTL을 늘리지 않으며 충돌 시 false다. */
    boolean saveTransaction(String stateDigest, Transaction transaction, Duration ttl);

    /**
     * 만료·공급자·브라우저 바인딩 확인과 일회성 소비를 원자 처리한다.
     * 없거나 불일치하면 empty이며, 바인딩이 다른 정상 상태를 삭제하지 않는다.
     * 후속 공급자/DB 요청이 실패해도 소비한 상태를 복원하지 않는다.
     */
    Optional<Transaction> consumeTransaction(String stateDigest, SocialProvider provider, String browserBindingDigest);

    boolean saveResult(String digest, String browserDigest, Result result, Duration ttl);

    Optional<Result> consumeResult(String digest, String browserDigest);

    Optional<SocialIdentity> consumeSignup(String digest, String browserDigest);

    enum Purpose {
        LOGIN, LINK
    }

    /** 연결 시작 시 확인한 계정. 콜백의 DB 조회에서도 활성 상태와 authVersion을 다시 검사한다. */
    record LinkingAccount(long userId, long authVersion) {
        public LinkingAccount {
            if (userId <= 0 || authVersion < 0) {
                throw new IllegalArgumentException("Invalid linking account");
            }
        }
    }

    /** returnPath는 저장 전에 서버 허용 목록으로 검증한다. LOGIN이면 linkingAccount는 null이다. */
    record Transaction(SocialProvider provider, Purpose purpose, String browserBindingDigest,
                       String returnPath, String codeVerifier, LinkingAccount linkingAccount) {
        public Transaction {
            Objects.requireNonNull(provider, "provider");
            Objects.requireNonNull(purpose, "purpose");
            if (browserBindingDigest == null || browserBindingDigest.isBlank()
                    || returnPath == null || returnPath.isBlank()
                    || codeVerifier == null || codeVerifier.isBlank()) {
                throw new IllegalArgumentException("OAuth transaction binding is required");
            }
            if ((purpose == Purpose.LINK) != (linkingAccount != null)) {
                throw new IllegalArgumentException("Only a link transaction requires a linking account");
            }
        }

        @Override
        public String toString() {
            return "Transaction[redacted]";
        }
    }

    enum Status { LOGIN_READY, AUTO_LINK_READY, SIGNUP_REQUIRED, FAILED }

    record Result(Status status, SocialIdentity identity, SocialAccountResult account, AuthErrorCode error) {
        @Override
        public String toString() { return "SocialAuthResult[redacted]"; }
    }
}
