package com.daebangg.sayroam.global.websocket;

import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 로그인 자격이 무효가 되면(비밀번호 변경·전체 로그아웃·탈퇴) 그 사용자의 연결을 모두 끊는다.
// 수신만 하는 연결은 다시 확인할 시점이 없으므로 커밋 직후 서버가 직접 끊는다.
@Component
@RequiredArgsConstructor
public class StompSessionRevoker {
    private final StompSessionCloser sessionCloser;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoginsInvalidated(LoginsInvalidated event) {
        sessionCloser.closeAllSessions(event.userId(), CommonErrorCode.UNAUTHENTICATED);
    }
}
