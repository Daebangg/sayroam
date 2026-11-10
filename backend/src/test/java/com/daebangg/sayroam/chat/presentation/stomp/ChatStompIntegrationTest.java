package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.auth.application.port.out.AccountDeletion;
import com.daebangg.sayroam.auth.application.port.out.PasswordChange;
import com.daebangg.sayroam.auth.infrastructure.config.AccessTokenProperties;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.room.application.RoomService;
import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.type.Role;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.entity.UserCredential;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatStompIntegrationTest extends DatabaseIntegrationTest {
    private static final Object PROBE = Map.of("probe", true);
    private static final String CLIENT_MESSAGE_ID = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10";
    private static final String EMAIL = "user@example.com";

    @LocalServerPort
    int port;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    AccessTokenIssuer accessTokenIssuer;

    @Autowired
    AccessTokenProperties accessTokenProperties;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    PasswordChange passwordChange;

    @Autowired
    AccountDeletion accountDeletion;

    @Autowired
    RoomService roomService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomMemberRepository roomMemberRepository;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAllInBatch();
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM UserCredential").executeUpdate();
            entityManager.createQuery("DELETE FROM User").executeUpdate();
        });
    }

    @Test
    void 유효한_토큰이면_연결된다() throws Exception {
        // given
        Long userId = saveUser();

        // when
        StompSession session = connect(tokenOf(userId)).get(5, TimeUnit.SECONDS);

        // then
        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    void 토큰_없이_연결하면_거부한다() {
        // when & then
        assertThatThrownBy(() -> connect(null).get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void 잘못된_토큰으로_연결하면_거부한다() {
        // when & then
        assertThatThrownBy(() -> connect("invalid-token").get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void 만료된_토큰으로_연결하면_거부한다() {
        // given
        Long userId = saveUser();
        String token = tokenExpiringAt(userId, Instant.now().minus(1, ChronoUnit.HOURS));

        // when & then
        assertThatThrownBy(() -> connect(token).get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void 다른_방을_구독하면_거부한다() throws Exception {
        // given
        Long ownerId = saveUser();
        Long otherUserId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(otherUserId), events).get(5, TimeUnit.SECONDS);

        // when
        session.subscribe("/topic/rooms/" + roomId, new StompSessionHandlerAdapter() {
        });

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 방_멤버면_방을_구독한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        StompSession session = connect(tokenOf(userId)).get(5, TimeUnit.SECONDS);
        String destination = "/topic/rooms/" + roomId;

        // when
        ReceivedFrames frames = new ReceivedFrames();
        session.subscribe(destination, frames);

        // then
        awaitSubscribed(frames, () -> messagingTemplate.convertAndSend(destination, PROBE));
        session.disconnect();
    }

    @Test
    void 허용되지_않은_destination을_구독하면_거부한다() throws Exception {
        // given
        Long userId = saveUser();
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);

        // when
        session.subscribe("/topic/admin", new StompSessionHandlerAdapter() {
        });

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/queue/messages", "/queue/errors"})
    void 자신의_개인_큐를_구독한다(String queue) throws Exception {
        // given
        Long userId = saveUser();
        StompSession session = connect(tokenOf(userId)).get(5, TimeUnit.SECONDS);

        // when
        ReceivedFrames frames = new ReceivedFrames();
        session.subscribe("/user" + queue, frames);

        // then
        awaitSubscribed(frames, () -> messagingTemplate.convertAndSendToUser(userId.toString(), queue, PROBE));
        session.disconnect();
    }

    @Test
    void 토픽으로_직접_전송하면_거부한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);

        // when
        session.send("/topic/rooms/" + roomId, Map.of("content", "위조"));

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 메시지를_보내면_같은_방_구독자에게_전달된다() throws Exception {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        joinRoom(roomId, memberId);
        StompSession ownerSession = connect(tokenOf(ownerId)).get(5, TimeUnit.SECONDS);
        ReceivedFrames memberFrames = subscribeRoom(connect(tokenOf(memberId)).get(5, TimeUnit.SECONDS), roomId);

        // when
        send(ownerSession, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        Map<String, Object> message = memberFrames.messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.get("messageId")).isNotNull();
        assertThat(message.get("senderId")).isEqualTo(ownerId.intValue());
        assertThat(message.get("clientMessageId")).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(message.get("content")).isEqualTo("안녕");
        assertThat(message.get("createdAt")).asString().endsWith("Z");
        assertThat(chatMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void 메시지를_보내면_보낸_사람에게_전송_결과를_전달한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        StompSession session = connect(tokenOf(userId)).get(5, TimeUnit.SECONDS);
        ReceivedFrames results = subscribeUserQueue(session, userId, "/queue/messages");

        // when
        send(session, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        Map<String, Object> result = results.messages.poll(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.get("messageId")).isNotNull();
        assertThat(result.get("clientMessageId")).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(result.get("createdAt")).asString().endsWith("Z");
    }

    @Test
    void 재전송하면_보낸_사람에게만_다시_전달된다() throws Exception {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        joinRoom(roomId, memberId);
        StompSession ownerSession = connect(tokenOf(ownerId)).get(5, TimeUnit.SECONDS);
        ReceivedFrames ownerResults = subscribeUserQueue(ownerSession, ownerId, "/queue/messages");
        ReceivedFrames memberFrames = subscribeRoom(connect(tokenOf(memberId)).get(5, TimeUnit.SECONDS), roomId);
        send(ownerSession, roomId, CLIENT_MESSAGE_ID, "안녕");
        Map<String, Object> first = ownerResults.messages.poll(5, TimeUnit.SECONDS);
        assertThat(memberFrames.messages.poll(5, TimeUnit.SECONDS)).isNotNull();

        // when
        send(ownerSession, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        Map<String, Object> resent = ownerResults.messages.poll(5, TimeUnit.SECONDS);
        assertThat(first).isNotNull();
        assertThat(resent).isNotNull();
        assertThat(resent.get("messageId")).isEqualTo(first.get("messageId"));
        assertThat(memberFrames.messages.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void 전송에_실패하면_클라이언트_ID와_오류_코드를_보낸_사람에게_전달한다() throws Exception {
        // given
        Long ownerId = saveUser();
        Long nonMemberId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        StompSession session = connect(tokenOf(nonMemberId)).get(5, TimeUnit.SECONDS);
        ReceivedFrames errors = subscribeUserQueue(session, nonMemberId, "/queue/errors");

        // when
        send(session, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        Map<String, Object> error = errors.messages.poll(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.get("clientMessageId")).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(error.get("code")).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED.getCode());
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    void 인증에_실패하면_ERROR_프레임에_인증_오류_코드를_담는다() throws Exception {
        // given
        ConnectionEvents events = new ConnectionEvents();

        // when
        connect(null, events);

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(CommonErrorCode.UNAUTHENTICATED.getCode());
    }

    @Test
    void 구독이_거부되면_ERROR_프레임에_거부_코드를_담는다() throws Exception {
        // given
        Long ownerId = saveUser();
        Long otherUserId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(otherUserId), events).get(5, TimeUnit.SECONDS);

        // when
        session.subscribe("/topic/rooms/" + roomId, new StompSessionHandlerAdapter() {
        });

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED.getCode());
    }

    @Test
    void 비밀번호_변경으로_인증_버전이_바뀌면_전송을_거부한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        increaseAuthVersion(userId);

        // when
        send(session, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(chatMessageRepository.count()).isZero();
    }

    @Test
    void 비밀번호_변경으로_인증_버전이_바뀌면_구독을_거부한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        increaseAuthVersion(userId);

        // when
        session.subscribe("/topic/rooms/" + roomId, new StompSessionHandlerAdapter() {
        });

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 탈퇴한_회원의_연결은_전송을_거부한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        withdrawUser(userId);

        // when
        send(session, roomId, CLIENT_MESSAGE_ID, "안녕");

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(chatMessageRepository.count()).isZero();
    }

    @Test
    void 토큰이_만료되면_수신만_하는_연결도_종료한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenExpiringAt(userId, Instant.now().plusSeconds(2)), events)
                .get(5, TimeUnit.SECONDS);

        // when
        subscribeRoom(session, roomId);

        // then
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 비밀번호를_변경하면_수신만_하는_연결도_즉시_종료한다() throws Exception {
        // given
        Long userId = saveVerifiedUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        subscribeRoom(session, roomId);

        // when
        passwordChange.change(userId, EMAIL, 0L, "new-hash");

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(CommonErrorCode.UNAUTHENTICATED.getCode());
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 탈퇴하면_수신만_하는_연결도_즉시_종료한다() throws Exception {
        // given
        Long userId = saveVerifiedUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        subscribeRoom(session, roomId);

        // when
        accountDeletion.delete(userId, EMAIL, 0L);

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(CommonErrorCode.UNAUTHENTICATED.getCode());
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 방을_나가면_그_방을_구독한_연결을_즉시_종료한다() throws Exception {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ConnectionEvents events = new ConnectionEvents();
        StompSession session = connect(tokenOf(userId), events).get(5, TimeUnit.SECONDS);
        subscribeRoom(session, roomId);

        // when
        roomService.leave(roomId, userId);

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED.getCode());
        assertThat(events.closed.get(5, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void 토큰이_만료되면_ERROR_프레임에_인증_오류_코드를_담는다() throws Exception {
        // given
        Long userId = saveUser();
        ConnectionEvents events = new ConnectionEvents();

        // when
        connect(tokenExpiringAt(userId, Instant.now().plusSeconds(1)), events).get(5, TimeUnit.SECONDS);

        // then
        StompHeaders error = events.errorFrame.get(5, TimeUnit.SECONDS);
        assertThat(error.getFirst("message")).isEqualTo(CommonErrorCode.UNAUTHENTICATED.getCode());
    }

    private void increaseAuthVersion(Long userId) {
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE User u SET u.authVersion = u.authVersion + 1 WHERE u.id = :id")
                .setParameter("id", userId)
                .executeUpdate());
    }

    private void withdrawUser(Long userId) {
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE User u SET u.deletedAt = :now WHERE u.id = :id")
                .setParameter("now", LocalDateTime.now(ZoneOffset.UTC))
                .setParameter("id", userId)
                .executeUpdate());
    }

    private ReceivedFrames subscribeUserQueue(StompSession session, Long userId, String queue) throws Exception {
        ReceivedFrames frames = new ReceivedFrames();
        session.subscribe("/user" + queue, frames);
        awaitSubscribed(frames, () -> messagingTemplate.convertAndSendToUser(userId.toString(), queue, PROBE));
        return frames;
    }

    private ReceivedFrames subscribeRoom(StompSession session, Long roomId) throws Exception {
        String destination = "/topic/rooms/" + roomId;
        ReceivedFrames frames = new ReceivedFrames();
        session.subscribe(destination, frames);
        awaitSubscribed(frames, () -> messagingTemplate.convertAndSend(destination, PROBE));
        return frames;
    }

    private void send(StompSession session, Long roomId, String clientMessageId, String content) {
        StompHeaders headers = new StompHeaders();
        headers.setDestination("/app/rooms/" + roomId + "/messages");
        headers.setContentType(MediaType.APPLICATION_JSON);
        session.send(headers, Map.of("clientMessageId", clientMessageId, "content", content));
    }

    private void awaitSubscribed(ReceivedFrames frames, Runnable publishProbe) throws Exception {
        for (int attempt = 0; attempt < 50 && !frames.subscribed.isDone(); attempt++) {
            publishProbe.run();
            try {
                frames.subscribed.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 구독 프레임은 비동기로 처리되므로 다시 확인한다.
            }
        }
        assertThat(frames.subscribed).isCompleted();
    }

    private String tokenOf(Long userId) {
        return accessTokenIssuer.issue(userId, 0).accessToken();
    }

    private String tokenExpiringAt(Long userId, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(accessTokenProperties.issuer())
                .audience(List.of(accessTokenProperties.audience()))
                .subject(userId.toString())
                .issuedAt(expiresAt.minus(1, ChronoUnit.MINUTES))
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("token_use", "access")
                .claim("auth_ver", "0")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("at+jwt").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private CompletableFuture<StompSession> connect(String token) {
        return connect(token, new ConnectionEvents());
    }

    private CompletableFuture<StompSession> connect(String token, ConnectionEvents events) {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers, events);
    }

    private void joinRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        roomMemberRepository.save(RoomMember.joinAsOwner(room, userId, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private static class ReceivedFrames extends StompSessionHandlerAdapter {
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
        final CompletableFuture<Throwable> closed = new CompletableFuture<>();
        final CompletableFuture<StompHeaders> errorFrame = new CompletableFuture<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errorFrame.complete(headers);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            closed.complete(exception);
        }
    }

    private Long saveVerifiedUser() {
        return transactionTemplate.execute(status -> {
            User user = User.verifiedEmailMember(EMAIL, "회원", LocalDateTime.now(ZoneOffset.UTC));
            entityManager.persist(user);
            entityManager.persist(UserCredential.builder().user(user).passwordHash("old-hash").build());
            return user.getId();
        });
    }

    private Long saveUser() {
        return transactionTemplate.execute(status -> {
            User user = User.builder().nickname("회원").role(Role.USER).build();
            entityManager.persist(user);
            return (Long) entityManagerFactory.getPersistenceUnitUtil().getIdentifier(user);
        });
    }
}
