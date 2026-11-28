package com.daebangg.sayroam.global.websocket;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private static final String USER_QUEUE_PREFIX = "/user/queue/";
    private static final String APPLICATION_PREFIX = "/app/";
    private final List<StompSubscriptionAuthorizer> subscriptionAuthorizers;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand command = accessor.getCommand();
        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor.getDestination(), (StompPrincipal) accessor.getUser());
        } else if (StompCommand.SEND.equals(command)) {
            authorizeSend(accessor.getDestination());
        }
        return message;
    }

    private void authorizeSubscribe(String destination, StompPrincipal principal) {
        if (destination == null) {
            throw new StompAccessDeniedException();
        }
        if (destination.startsWith(USER_QUEUE_PREFIX)) {
            return;
        }
        StompSubscriptionAuthorizer authorizer = subscriptionAuthorizers.stream()
                .filter(candidate -> candidate.supports(destination))
                .findFirst()
                .orElseThrow(StompAccessDeniedException::new);
        authorizer.authorize(destination, principal.userId());
    }

    private void authorizeSend(String destination) {
        if (destination == null || !destination.startsWith(APPLICATION_PREFIX)) {
            throw new StompAccessDeniedException();
        }
    }

}
