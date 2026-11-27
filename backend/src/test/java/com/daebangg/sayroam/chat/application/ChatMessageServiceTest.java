package com.daebangg.sayroam.chat.application;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.application.dto.ChatMessageSummaryResult;
import com.daebangg.sayroam.chat.application.service.ChatMessageService;
import com.daebangg.sayroam.chat.domain.ChatMessage;
import com.daebangg.sayroam.chat.domain.error.ChatErrorCode;
import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import com.daebangg.sayroam.chat.infrastructure.persistence.ChatMessageJpaRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
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
    ChatMessageJpaRepository chatMessageRepository;

    @Autowired
    RoomService roomService;

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    private RoomMemberJpaRepository roomMemberRepository;

    @Autowired
    private RoomJpaRepository roomRepository;

    @Autowired
    ApplicationEvents events;

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAllInBatch();
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }


    @Test
    void 멤버가_아니면_메시지_전송을_거부한다() {
        // given
        Long ownerId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
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
        Long roomId = createRoomJoinedBy(ownerId);

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
        Long roomId = createRoomJoinedBy(ownerId);
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
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, senderId);
        chatMessageService.send(roomId, ownerId, CLIENT_MESSAGE_ID, content);

        chatMessageService.send(roomId, senderId, CLIENT_MESSAGE_ID, content);

        assertThat(chatMessageRepository.count()).isEqualTo(2);
    }

    @Test
    void 새_메시지를_저장하면_전송_이벤트를_발행한다() {
        // given
        String content = "dad";
        Long userId = saveUser();
        Long roomId = createRoomJoinedBy(userId);

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
        Long roomId = createRoomJoinedBy(userId);
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
        Long roomId = createRoomJoinedBy(userId);
        chatMessageRepository.save(ChatMessage.text(roomId, userId, CLIENT_MESSAGE_ID, "안녕"));

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.save(ChatMessage.text(roomId, userId, CLIENT_MESSAGE_ID, "안녕")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void afterId_이후_메시지를_ID_오름차순으로_조회한다() {
        // given
        Long userId = saveUser();
        Long roomId = createRoomJoinedBy(userId);
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
        Long roomId = createRoomJoinedBy(userId);
        Long otherRoomId = createRoomJoinedBy(userId);
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
        Long roomId = createRoomJoinedBy(ownerId);

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
        Long roomId = createRoomJoinedBy(ownerId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "1");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "2");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "3");
        joinRoom(roomId, otherId);
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
        Long roomId = createRoomJoinedBy(userId);
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
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "재입장 전");
        roomService.leave(roomId, memberId);
        joinRoom(roomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "재입장 후");

        // when
        List<ChatMessageResult> messages = chatMessageService.findAfter(roomId, memberId, 0L, 10);

        // then
        assertThat(messages)
                .extracting(ChatMessageResult::content)
                .containsExactly("재입장 후");
    }

    @Test
    void 처음_읽기_전이면_입장_후_다른_사람이_보낸_메시지를_모두_센다() {
        // given
        Long memberId = saveUser();
        Long otherId = saveUser();
        Long roomId = createRoomJoinedBy(memberId);
        joinRoom(roomId, otherId);
        chatMessageService.send(roomId, memberId, UUID.randomUUID().toString(), "1");
        chatMessageService.send(roomId, memberId, UUID.randomUUID().toString(), "2");

        // when
        long unreadCount = chatMessageService.getSummary(roomId, otherId).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(2);

    }

    @Test
    void 내가_보낸_메시지는_세지_않는다() {
        // given
        Long memberId = saveUser();
        Long myId = saveUser();
        Long roomId = createRoomJoinedBy(memberId);
        joinRoom(roomId, myId);
        chatMessageService.send(roomId, memberId, UUID.randomUUID().toString(), "남의 메시지");
        chatMessageService.send(roomId, myId, UUID.randomUUID().toString(), "내 메시지");

        // when
        long unreadCount = chatMessageService.getSummary(roomId, myId).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void 입장_전_메시지는_세지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "입장 전");
        joinRoom(roomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "입장 후");

        // when
        long unreadCount = chatMessageService.getSummary(roomId, memberId).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void 다른_방의_메시지는_세지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        Long otherRoomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        joinRoom(otherRoomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "제주 방");
        chatMessageService.send(otherRoomId, ownerId, UUID.randomUUID().toString(), "부산 방");

        // when
        long unreadCount = chatMessageService.getSummary(roomId, memberId).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void 멤버가_아니면_안_읽은_개수_조회를_거부한다() {
        // given
        Long ownerId = saveUser();
        Long nonMemberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);

        // when & then
        assertThatThrownBy(() -> chatMessageService.getSummary(roomId, nonMemberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 읽은_위치_이후_메시지만_센다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        ChatMessageResult read = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "읽음");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "안 읽음");
        roomService.advanceLastRead(roomId, memberId, read.messageId());

        // when
        long unreadCount = chatMessageService.getSummary(roomId, memberId).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void 요약에_개수를_센_시점의_마지막_메시지_ID를_함께_돌려준다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "남의 메시지");
        ChatMessageResult mine = chatMessageService.send(roomId, memberId, UUID.randomUUID().toString(), "내 메시지");

        // when
        ChatMessageSummaryResult summary = chatMessageService.getSummary(roomId, memberId);

        // then
        assertThat(summary.unreadCount()).isEqualTo(1);
        assertThat(summary.lastMessageId()).isEqualTo(mine.messageId());
    }

    @Test
    void 기준_메시지_이후의_메시지는_안_읽은_개수에_세지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "1");
        ChatMessageResult until = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "2");
        chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "기준 이후");
        LocalDateTime joinedAt = roomService.getJoinedAt(roomId, memberId);

        // when
        long unreadCount = chatMessageRepository.countUnread(roomId, memberId, null, until.messageId(), joinedAt);

        // then
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    void 메시지가_없으면_요약의_마지막_메시지_ID는_0이다() {
        // given
        Long userId = saveUser();
        Long roomId = createRoomJoinedBy(userId);

        // when
        ChatMessageSummaryResult summary = chatMessageService.getSummary(roomId, userId);

        // then
        assertThat(summary.unreadCount()).isZero();
        assertThat(summary.lastMessageId()).isZero();
    }

    @Test
    void 방의_메시지로_읽은_위치를_옮긴다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        ChatMessageResult message = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "안녕");

        // when
        chatMessageService.advanceLastRead(roomId, memberId, message.messageId());

        // then
        assertThat(lastReadMessageId(roomId, memberId)).isEqualTo(message.messageId());
    }

    @Test
    void 없는_메시지로는_읽은_위치를_옮길_수_없다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        ChatMessageResult read = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "읽음");
        chatMessageService.advanceLastRead(roomId, memberId, read.messageId());

        // when & then
        assertThatThrownBy(() -> chatMessageService.advanceLastRead(roomId, memberId, Long.MAX_VALUE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
        assertThat(lastReadMessageId(roomId, memberId)).isEqualTo(read.messageId());
    }

    @Test
    void 다른_방의_메시지로는_읽은_위치를_옮길_수_없다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        Long otherRoomId = createRoomJoinedBy(ownerId);
        joinRoom(roomId, memberId);
        ChatMessageResult read = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "제주 방");
        ChatMessageResult otherRoomMessage =
                chatMessageService.send(otherRoomId, ownerId, UUID.randomUUID().toString(), "부산 방");
        chatMessageService.advanceLastRead(roomId, memberId, read.messageId());

        // when & then
        assertThatThrownBy(() -> chatMessageService.advanceLastRead(roomId, memberId, otherRoomMessage.messageId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
        assertThat(lastReadMessageId(roomId, memberId)).isEqualTo(read.messageId());
    }

    @Test
    void 멤버가_아니면_읽은_위치를_옮길_수_없다() {
        // given
        Long ownerId = saveUser();
        Long nonMemberId = saveUser();
        Long roomId = createRoomJoinedBy(ownerId);
        ChatMessageResult message = chatMessageService.send(roomId, ownerId, UUID.randomUUID().toString(), "안녕");

        // when & then
        assertThatThrownBy(() -> chatMessageService.advanceLastRead(roomId, nonMemberId, message.messageId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    private Long lastReadMessageId(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId).orElseThrow().getLastReadMessageId();
    }

    private void joinRoom(Long roomId, Long userId) {
        roomService.join(roomId, userId);
    }

    private Long saveUser() {
        User user = User.builder().nickname("방장").role(Role.USER).build();
        return userRepository.save(user).getId();
    }

    private Long createRoomJoinedBy(Long userId) {
        Long roomId = roomService.createForTrip();
        roomService.join(roomId, userId);
        return roomId;
    }
}
