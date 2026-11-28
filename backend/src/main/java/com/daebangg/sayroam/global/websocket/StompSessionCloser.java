package com.daebangg.sayroam.global.websocket;

import com.daebangg.sayroam.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Predicate;

// 서버가 클라이언트의 STOMP 연결을 끊는다. ERROR 프레임을 받으면 Spring이 전송 후 연결을 닫는다.
// 끊을지는 판단하지 않고 호출한 쪽이 정한 연결을 끊기만 한다. 사용자 한 명이 탭·기기마다 세션을 따로 가질 수 있다.
@Slf4j
@Component
public class StompSessionCloser {
    private final MessageChannel clientOutboundChannel;
    private final SimpUserRegistry userRegistry;

    public StompSessionCloser(@Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel,
                              SimpUserRegistry userRegistry) {
        this.clientOutboundChannel = clientOutboundChannel;
        this.userRegistry = userRegistry;
    }

    public void close(String sessionId, ErrorCode errorCode) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setSessionId(sessionId);
        accessor.setMessage(errorCode.getCode());
        accessor.setLeaveMutable(true);
        clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
    }

    public void closeAllSessions(Long userId, ErrorCode errorCode) {
        closeSessions(userId, session -> true, errorCode);
    }

    public void closeSessionsSubscribedTo(Long userId, Set<String> destinations, ErrorCode errorCode) {
        closeSessions(userId, session -> session.getSubscriptions().stream()
                .anyMatch(subscription -> destinations.contains(subscription.getDestination())), errorCode);
    }

    private void closeSessions(Long userId, Predicate<SimpSession> target, ErrorCode errorCode) {
        // StompPrincipal.getName()이 userId 문자열이라 이 이름으로 찾는다.
        SimpUser user = userRegistry.getUser(userId.toString());
        if (user == null) {
            return;
        }
        user.getSessions().stream()
                .filter(target)
                .forEach(session -> closeQuietly(session.getId(), errorCode));
    }

    // 한 세션이 실패해도 나머지는 끊는다. 끊지 못한 연결은 늦어도 토큰 만료 시각에 끊긴다.
    private void closeQuietly(String sessionId, ErrorCode errorCode) {
        try {
            close(sessionId, errorCode);
        } catch (RuntimeException exception) {
            log.warn("Failed to close STOMP session: {}", sessionId, exception);
        }
    }
}
