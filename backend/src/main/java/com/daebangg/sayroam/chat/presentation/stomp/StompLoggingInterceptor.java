package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.global.logging.WebSocketLog;
import org.slf4j.event.Level;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** 인증 뒤의 프레임과 실제 STOMP 세션 생명주기를 기록한다. */
@Component
public class StompLoggingInterceptor implements ChannelInterceptor {
    private final Map<String, Long> connectedAt = new ConcurrentHashMap<>();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (!(event.getUser() instanceof ChatPrincipal) || sessionId == null
                || connectedAt.putIfAbsent(sessionId, System.nanoTime()) != null) return;
        WebSocketLog.event(Level.INFO, "websocket_connected", "CONNECTED", sessionId).log("WebSocket connected");
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Long startedAt = connectedAt.remove(event.getSessionId());
        // Spring은 같은 세션의 종료 이벤트를 여러 번 발행할 수 있다.
        if (startedAt == null) return;
        int closeCode = event.getCloseStatus().getCode();
        Level level = closeCode == 1000 || closeCode == 1001 ? Level.INFO : Level.WARN;
        WebSocketLog.event(level, "websocket_disconnected", "DISCONNECTED", event.getSessionId())
                .addKeyValue("closeCode", closeCode)
                .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt))
                .log("WebSocket disconnected");
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (!sent || headers == null) return;
        StompCommand command = headers.getCommand();
        if (command != StompCommand.SUBSCRIBE && command != StompCommand.UNSUBSCRIBE && command != StompCommand.SEND) return;
        WebSocketLog.event(Level.DEBUG, "websocket_frame_accepted", command.name(), headers.getSessionId())
                .addKeyValue("route", routeOf(headers.getDestination())).log("WebSocket inbound frame accepted");
    }

    private String routeOf(String destination) {
        if (destination == null) return "UNMAPPED";
        if (destination.matches("/topic/rooms/\\d+(/members)?")) return destination.replaceFirst("/\\d+", "/{roomId}");
        if (destination.matches("/app/rooms/\\d+/messages")) return "/app/rooms/{roomId}/messages";
        return switch (destination) {
            case "/user/queue/messages", "/user/queue/errors" -> destination;
            default -> "UNMAPPED";
        };
    }
}
