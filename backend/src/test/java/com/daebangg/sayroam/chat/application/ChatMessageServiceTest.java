package com.daebangg.sayroam.chat.application;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.domain.ChatMessage;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomInvitationJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.repository.UserRepository;
import com.daebangg.sayroam.user.domain.type.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class ChatMessageServiceTest extends DatabaseIntegrationTest {

    private static final String CLIENT_MESSAGE_ID = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10";

    @Autowired
    ChatMessageService chatMessageService;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    RoomService roomService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private RoomMemberJpaRepository roomMemberRepository;

    @Autowired
    private RoomJpaRepository roomRepository;

    @Autowired
    private RoomInvitationJpaRepository roomInvitationRepository;

    @Autowired
    ApplicationEvents events;

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAllInBatch();
        roomInvitationRepository.deleteAllInBatch();
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }


    @Test
    void 멤버가_아니면_메시지_전송을_거부한다() {
        // given
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        Long nonMemberId = saveUser();

        assertThatThrownBy(() -> chatMessageService.send(roomId, nonMemberId, CLIENT_MESSAGE_ID, "안녕"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));

        assertThat(chatMessageRepository.count()).isZero();
    }

    @Test
    void 참여_중인_멤버가_보낸_메시지를_저장한다() {
        // given
        String content = "안녕";
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");

        // when
        chatMessageService.send(roomId, ownerId, CLIENT_MESSAGE_ID, content);

        // then
        assertThat(chatMessageRepository.findAll())
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.getRoomId()).isEqualTo(roomId);
                    assertThat(message.getSenderId()).isEqualTo(ownerId);
                    assertThat(message.getClientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
                    assertThat(message.getContent()).isEqualTo(content);
                });
    }

    @Test
    void 같은_클라이언트_ID로_재전송하면_한_번만_저장한다() {
        // given
        String content = "hello";
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");
        chatMessageService.send(roomId, ownerId, CLIENT_MESSAGE_ID, content);

        chatMessageService.send(roomId, ownerId, CLIENT_MESSAGE_ID, content);

        assertThat(chatMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void 다른_회원이_같은_클라이언트_ID를_보내면_각각_저장한다() {
        // given
        String content = "hello";
        Long ownerId = saveUser();
        Long senderId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");
        joinRoom(roomId, ownerId, senderId);
        chatMessageService.send(roomId, ownerId, CLIENT_MESSAGE_ID, content);

        chatMessageService.send(roomId, senderId, CLIENT_MESSAGE_ID, content);

        assertThat(chatMessageRepository.count()).isEqualTo(2);
    }

    @Test
    void 새_메시지를_저장하면_전송_이벤트를_발행한다() {
        // given
        String content = "dad";
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 1박 2일");

        // when
        chatMessageService.send(roomId, userId, CLIENT_MESSAGE_ID, content);

        // then
        assertThat(events.stream(ChatMessageSent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.messageId()).isNotNull();
                    assertThat(event.roomId()).isEqualTo(roomId);
                    assertThat(event.senderId()).isEqualTo(userId);
                    assertThat(event.clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
                    assertThat(event.content()).isEqualTo("dad");
                });
    }

    @Test
    void 같은_클라이언트_ID로_재전송하면_저장된_값을_반환한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        ChatMessageResult first = chatMessageService.send(roomId, userId, CLIENT_MESSAGE_ID, "원본");
        events.clear();

        // when
        ChatMessageResult message = chatMessageService.send(roomId, userId, CLIENT_MESSAGE_ID, "변경");

        // then
        assertThat(message.messageId()).isEqualTo(first.messageId());
        assertThat(message.senderId()).isEqualTo(userId);
        assertThat(message.roomId()).isEqualTo(roomId);
        assertThat(message.clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(message.content()).isEqualTo("원본");
        assertThat(events.stream(ChatMessageSent.class)).isEmpty();
    }

    @Test
    void 같은_회원과_클라이언트_ID의_메시지는_DB에_중복_저장할_수_없다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        chatMessageRepository.save(ChatMessage.text(roomId, userId, CLIENT_MESSAGE_ID, "안녕"));

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.save(ChatMessage.text(roomId, userId, CLIENT_MESSAGE_ID, "안녕")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void afterId_이후_메시지를_ID_오름차순으로_조회한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주");
        ChatMessageResult first = chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "1");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "2");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "3");

        // when
        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, userId, first.messageId(), 10);

        // then
        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("2", "3");

    }

    @Test
    void 다른_방의_메시지는_조회되지_않는다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주");
        Long otherRoomId = roomService.create(userId, "부산");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "제주 방");
        chatMessageService.send(otherRoomId, userId, UUID.randomUUID().toString(), "부산 방");

        // when
        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, userId, 0L, 10);

        // then
        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("제주 방");

    }

    @Test
    void 멤버가_아니면_메시지_조회를_거부한다() {
        // given
        Long ownerId = saveUser();
        Long nonMemberId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");

        assertThatThrownBy(() ->
                chatMessageService.findAfter(roomId, nonMemberId, 0L, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 입장_전_메시지는_조회되지_않는다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "1");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "2");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "3");
        joinRoom(roomId, ownerId, otherId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "4");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "5");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "6");

        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, otherId, 0L, 10);

        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("4", "5", "6");


    }

    @Test
    void 조회_개수를_넘는_메시지는_반환하지_않는다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "1");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "2");
        chatMessageService.send(roomId, userId, UUID.randomUUID().toString(), "3");

        // when
        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, userId, 0L, 2);

        // then
        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("1", "2");
    }

    @Test
    void 다시_입장하면_재입장_이후_메시지만_조회된다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        joinRoom(roomId, ownerId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "재입장 전");
        roomService.leave(roomId, memberId);
        joinRoom(roomId, ownerId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "재입장 후");

        // when
        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, memberId, 0L, 10);

        // then
        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("재입장 후");
    }

    private void joinRoom(Long roomId, Long ownerId, Long userId) {
        String code = roomService.createInvitation(roomId, ownerId);
        roomService.joinByInvitation(code, userId);
    }

    private Long saveUser() {
        User user = User.builder().nickname("방장").role(Role.USER).build();
        return userRepository.save(user).getId();
    }
}