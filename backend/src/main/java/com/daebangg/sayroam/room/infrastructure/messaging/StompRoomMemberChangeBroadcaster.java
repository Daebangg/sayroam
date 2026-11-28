package com.daebangg.sayroam.room.infrastructure.messaging;

import com.daebangg.sayroam.room.application.port.out.RoomMemberChangeBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompRoomMemberChangeBroadcaster implements RoomMemberChangeBroadcaster {
    // Map 타입 그대로 넘기면 헤더를 받는 convertAndSend(Object, Map) 오버로드와 겹친다.
    private static final Object EMPTY_PAYLOAD = Map.of();

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(Long roomId) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/members", EMPTY_PAYLOAD);
    }
}
