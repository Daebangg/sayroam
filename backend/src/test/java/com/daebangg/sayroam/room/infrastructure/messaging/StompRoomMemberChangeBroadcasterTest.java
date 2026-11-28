package com.daebangg.sayroam.room.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class StompRoomMemberChangeBroadcasterTest {
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StompRoomMemberChangeBroadcaster broadcaster = new StompRoomMemberChangeBroadcaster(messagingTemplate);

    @Test
    void 방_멤버_채널로_빈_신호를_보낸다() {
        // when
        broadcaster.broadcast(5L);

        // then
        then(messagingTemplate).should().convertAndSend("/topic/rooms/5/members", (Object) Map.of());
    }
}
