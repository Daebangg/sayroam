package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.type.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatStompStoreFailureTest extends DatabaseIntegrationTest {
    private static final long USER_ID = 7L;
    private static final String CLIENT_MESSAGE_ID = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10";

    @LocalServerPort
    int port;

    @MockitoBean
    AccountLookup accountLookup;

    @Autowired
    AccessTokenIssuer accessTokenIssuer;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Test
    void CONNECT_중_저장소_장애면_일시_장애_코드를_전달한다() throws Exception {
        // given
        given(accountLookup.findActiveAccount(USER_ID)).willThrow(storeFailure());
        ConnectionEvents events = new ConnectionEvents();

        // when
        connect(events);

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(AuthErrorCode.AUTHENTICATION_UNAVAILABLE.getCode());
    }

    @Test
    void SEND_중_저장소_장애면_실패만_알리고_연결을_유지한다() throws Exception {
        // given
        given(accountLookup.findActiveAccount(USER_ID)).willReturn(Optional.of(activeAccount()));
        StompSession session = connect(new ConnectionEvents()).get(5, TimeUnit.SECONDS);
        ReceivedErrors errors = subscribeErrors(session);
        given(accountLookup.findActiveAccount(USER_ID)).willThrow(storeFailure());

        // when
        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/rooms/1/messages");
        headers.setContentType(MediaType.APPLICATION_JSON);
        session.send(headers, Map.of("clientMessageId", CLIENT_MESSAGE_ID, "content", "안녕"));

        // then
        Map<String, Object> error = errors.messages.poll(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("clientMessageId")).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(error.get("code")).isEqualTo(AuthErrorCode.AUTHENTICATION_UNAVAILABLE.getCode());
        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    private ReceivedErrors subscribeErrors(StompSession session) throws Exception {
        ReceivedErrors errors = new ReceivedErrors();
        session.subscribe("/user/queue/errors", errors);
        for (int attempt = 0; attempt < 50 && !errors.subscribed.isDone(); attempt++) {
            Object probe = Map.of("probe", true);
            messagingTemplate.convertAndSendToUser(Long.toString(USER_ID), "/queue/errors", probe);
            try {
                errors.subscribed.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 구독 프레임은 비동기로 처리되므로 다시 확인한다.
            }
        }
        assertThat(errors.subscribed).isCompleted();
        return errors;
    }

    private CompletableFuture<StompSession> connect(ConnectionEvents events) {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + accessTokenIssuer.issue(USER_ID, 0).accessToken());
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers, events);
    }

    private AuthException storeFailure() {
        return new AuthException(AuthErrorCode.AUTHENTICATION_UNAVAILABLE, new IllegalStateException("store down"));
    }

    private AuthenticatedAccount activeAccount() {
        return new AuthenticatedAccount(USER_ID, "user@example.com", "회원", null, Role.USER, 0L);
    }

    private static class ReceivedErrors extends StompSessionHandlerAdapter {
        final CompletableFuture<Void> subscribed = new CompletableFuture<>();
        final BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleFrame(StompHeaders headers, Object payload) {
            Map<String, Object> message = (Map<String, Object>) payload;
            if (message.containsKey("probe")) {
                subscribed.complete(null);
                return;
            }
            messages.add(message);
        }
    }

    private static class ConnectionEvents extends StompSessionHandlerAdapter {
        final CompletableFuture<StompHeaders> errorFrame = new CompletableFuture<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errorFrame.complete(headers);
        }
    }
}
