package com.daebangg.sayroam.global.websocket;

import com.daebangg.sayroam.auth.application.service.AccountAuthenticationService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder accessTokenDecoder;
    private final ActiveAccountJwtConverter accountConverter;
    private final AccountAuthenticationService accountAuthenticationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            StompPrincipal principal = validPrincipalOf(accessor);
            // 연결 뒤 비밀번호 변경·전체 로그아웃·탈퇴로 무효가 된 자격은 종료 이벤트가 오기 전이라도 막는다.
            accountAuthenticationService.ensureActive(principal.userId(), principal.authVersion());
        } else if (StompCommand.SEND.equals(command)) {
            validPrincipalOf(accessor);
        }
        return message;
    }

    private StompPrincipal authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new StompAuthenticationException();
        }

        try {
            Jwt jwt = accessTokenDecoder.decode(header.substring(BEARER_PREFIX.length()));
            AuthenticatedAccount account = (AuthenticatedAccount) accountConverter.convert(jwt).getPrincipal();
            return new StompPrincipal(account.userId(), account.authVersion(), jwt.getExpiresAt());
        } catch (AuthenticationServiceException e) {
            throw e;
        } catch (JwtException | AuthenticationException e) {
            throw new StompAuthenticationException();
        }
    }

    private StompPrincipal validPrincipalOf(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new StompAuthenticationException();
        }
        principal.ensureNotExpired(Instant.now());
        return principal;
    }

}
