package com.daebangg.sayroam.chat.infrastructure.messaging;

import com.daebangg.sayroam.chat.application.port.out.ChatConnectionCloser;
import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.logging.WebSocketLog;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

// ERROR 프레임을 받으면 Spring이 전송 후 연결을 닫는다.
@Component
public class StompChatConnectionCloser implements ChatConnectionCloser {
    private final MessageChannel clientOutboundChannel;

    public StompChatConnectionCloser(@Qualifier("clientOutboundChannel") MessageChannel clientOutboundChannel) {
        this.clientOutboundChannel = clientOutboundChannel;
    }

    @Override
    public void close(String sessionId, ErrorCode errorCode) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setSessionId(sessionId);
        accessor.setMessage(errorCode.getCode());
        accessor.setLeaveMutable(true);
        boolean sent = clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        WebSocketLog.event(sent ? Level.INFO : Level.WARN, sent ? "websocket_close_requested" : "websocket_close_not_sent",
                sent ? "CLOSE" : "CLOSE_FAILED", sessionId).addKeyValue("errorCode", errorCode.getCode())
                .log("WebSocket close requested");
    }
}
