package com.daebangg.sayroam.chat.infrastructure.messaging;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 구독자에게 메시지를 전달하는 내장 브로커 설정. 진입점 설정은 presentation의 WebSocketConfig가 맡는다.
@Configuration
public class StompBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
    }
}
