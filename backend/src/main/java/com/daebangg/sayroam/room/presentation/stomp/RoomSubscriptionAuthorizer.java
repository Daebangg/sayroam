package com.daebangg.sayroam.room.presentation.stomp;

import com.daebangg.sayroam.global.websocket.StompSubscriptionAuthorizer;
import com.daebangg.sayroam.room.application.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RoomSubscriptionAuthorizer implements StompSubscriptionAuthorizer {
    private static final Pattern ROOM_TOPIC = Pattern.compile("/topic/rooms/(\\d+)(/members)?");

    private final RoomService roomService;

    @Override
    public boolean supports(String destination) {
        return ROOM_TOPIC.matcher(destination).matches();
    }

    @Override
    public void authorize(String destination, Long userId) {
        Matcher matcher = ROOM_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a room subscription destination: " + destination);
        }
        roomService.ensureJoined(Long.valueOf(matcher.group(1)), userId);
    }
}
