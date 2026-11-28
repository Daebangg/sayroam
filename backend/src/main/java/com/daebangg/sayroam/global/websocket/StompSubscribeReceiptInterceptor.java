package com.daebangg.sayroam.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

// 내장 브로커는 SUBSCRIBE의 receipt에 답하지 않아, 브로커가 구독을 등록한 직후 대신 RECEIPT를 보낸다.
// /user 구독은 주소를 바꿔 다른 채널로 다시 등록되므로 이 시점에 등록됐다고 볼 수 없어 RECEIPT를 보내지 않는다.
class StompSubscribeReceiptInterceptor implements ExecutorChannelInterceptor {
    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
        if (ex != null || !(handler instanceof SimpleBrokerMessageHandler broker)) {
            return;
        }
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || accessor.getReceipt() == null) {
            return;
        }
        if (!registeredBy(broker, accessor.getDestination())) {
            return;
        }
        StompHeaderAccessor receipt = StompHeaderAccessor.create(StompCommand.RECEIPT);
        receipt.setSessionId(accessor.getSessionId());
        receipt.setReceiptId(accessor.getReceipt());
        receipt.setLeaveMutable(true);
        broker.getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, receipt.getMessageHeaders()));
    }

    private boolean registeredBy(SimpleBrokerMessageHandler broker, String destination) {
        return destination != null && broker.getDestinationPrefixes().stream().anyMatch(destination::startsWith);
    }
}
