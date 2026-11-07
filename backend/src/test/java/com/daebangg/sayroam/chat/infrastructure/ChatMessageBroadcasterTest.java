package com.daebangg.sayroam.chat.infrastructure;

import com.daebangg.sayroam.chat.application.ChatMessageService;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import com.daebangg.sayroam.room.application.RoomService;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

class ChatMessageBroadcasterTest extends DatabaseIntegrationTest {
    private static final String CLIENT_MESSAGE_ID = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10";

    @MockitoBean
    SimpMessagingTemplate messagingTemplate;

    @Autowired
    ChatMessageService chatMessageService;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    RoomService roomService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomMemberRepository roomMemberRepository;

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
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createQuery("DELETE FROM User").executeUpdate());
    }

    @Test
    void 커밋되면_방_구독자에게_메시지를_전달한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주");

        // when
        chatMessageService.send(roomId, userId, CLIENT_MESSAGE_ID, "1");

        // then
        ArgumentCaptor<ChatMessageSent> event = ArgumentCaptor.forClass(ChatMessageSent.class);
        then(messagingTemplate).should().convertAndSend(eq("/topic/rooms/" + roomId), event.capture());
        assertThat(event.getValue().content()).isEqualTo("1");
    }

    @Test
    void 저장이_롤백되면_메시지를_전달하지_않는다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");

        // when
        transactionTemplate.executeWithoutResult(status -> {
            chatMessageService.send(roomId, userId, CLIENT_MESSAGE_ID, "1");
            status.setRollbackOnly();
        });

        // then
        then(messagingTemplate).shouldHaveNoInteractions();
        assertThat(chatMessageRepository.count()).isZero();
    }

    private Long saveUser() {
        return transactionTemplate.execute(status -> {
            User user = User.builder().nickname("방장").role(Role.USER).build();
            entityManager.persist(user);
            return (Long) entityManagerFactory.getPersistenceUnitUtil().getIdentifier(user);
        });
    }
}