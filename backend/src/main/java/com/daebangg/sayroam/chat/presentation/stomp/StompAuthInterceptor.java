package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.room.application.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final Pattern ROOM_TOPIC = Pattern.compile("/topic/rooms/(\\d+)");
    private static final Pattern ROOM_MESSAGES = Pattern.compile("/app/rooms/\\d+/messages");
    private static final Set<String> USER_QUEUES = Set.of("/user/queue/messages", "/user/queue/errors");
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder accessTokenDecoder;
    private final ActiveAccountJwtConverter accountConverter;
    private final RoomService roomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }
        return message;
    }

    private ChatPrincipal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new StompAuthenticationException();
        }

        try {
            Jwt jwt = accessTokenDecoder.decode(header.substring(BEARER_PREFIX.length()));
            AuthenticatedAccount account = (AuthenticatedAccount) accountConverter.convert(jwt).getPrincipal();
            return new ChatPrincipal(account.userId(), jwt.getExpiresAt());
        } catch (JwtException | AuthenticationException e) {
            throw new StompAuthenticationException();
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        ChatPrincipal principal = principalOf(accessor);
        principal.ensureNotExpired(Instant.now());

        Matcher roomTopic = ROOM_TOPIC.matcher(accessor.getDestination());
        if (roomTopic.matches()) {
            roomService.ensureJoined(Long.valueOf(roomTopic.group(1)), principal.userId());
            return;
        }
        if (!USER_QUEUES.contains(accessor.getDestination())) {
            throw new StompAccessDeniedException();
        }
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        principalOf(accessor).ensureNotExpired(Instant.now());
        if (!ROOM_MESSAGES.matcher(accessor.getDestination()).matches()) {
            throw new StompAccessDeniedException();
        }
    }

    private ChatPrincipal principalOf(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof ChatPrincipal principal) {
            return principal;
        }
        throw new StompAuthenticationException();
    }
}
