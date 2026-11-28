package com.daebangg.sayroam.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// STOMP 엔드포인트와 내장 브로커 설정
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthInterceptor stompAuthInterceptor;
    private final StompAuthorizationInterceptor stompAuthorizationInterceptor;
    private final StompErrorHandler stompErrorHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
        registry.setErrorHandler(stompErrorHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    // preSend는 등록 순서대로 실행된다. 권한은 인증이 붙인 principal을 쓰므로 인증 다음에 둔다.
    // RECEIPT는 브로커가 구독을 등록한 뒤(afterMessageHandled)에 동작하므로 위치와 무관하다.
    // 클라이언트는 구독 RECEIPT를 받은 뒤 명단·메시지를 조회해 그 사이의 변경을 놓치지 않는다.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor, stompAuthorizationInterceptor,
                new StompSubscribeReceiptInterceptor());
    }
}
