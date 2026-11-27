package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.chat.application.port.out.ChatConnectionCloser;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.logging.LoggingTaskDecorator;
import com.daebangg.sayroam.global.logging.SafeLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

// 수신만 하는 연결은 인터셉터를 거치지 않으므로 토큰 만료 시각에 서버가 직접 연결을 끊는다.
@Component
@Slf4j
public class StompSessionExpiryScheduler {
    private final TaskScheduler taskScheduler;
    private final ChatConnectionCloser sessionCloser;
    private final Map<String, ScheduledFuture<?>> expirations = new ConcurrentHashMap<>();

    public StompSessionExpiryScheduler(@Qualifier("messageBrokerTaskScheduler") TaskScheduler taskScheduler,
                                       ChatConnectionCloser sessionCloser) {
        this.taskScheduler = taskScheduler;
        this.sessionCloser = sessionCloser;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        if (event.getUser() instanceof ChatPrincipal principal) {
            String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
            expirations.put(sessionId, taskScheduler.schedule(new LoggingTaskDecorator().decorate(() -> {
                // 연결 종료 이벤트가 먼저 처리된 경우에도 항목이 남지 않게 한다.
                expirations.remove(sessionId);
                try {
                    sessionCloser.close(sessionId, CommonErrorCode.UNAUTHENTICATED);
                } catch (RuntimeException exception) {
                    SafeLog.atWarn(log, exception).addKeyValue("event", "stomp_session_expiry_failed")
                            .log("Failed to close expired STOMP session");
                }
            }), principal.expiresAt()));
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        ScheduledFuture<?> expiration = expirations.remove(event.getSessionId());
        if (expiration != null) {
            expiration.cancel(false);
        }
    }
}
