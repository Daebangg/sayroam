package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.room.application.RoomService;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomRequest;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomApi {
    private final RoomService roomService;

    @Override
    @PostMapping(value = "/api/rooms", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateRoomResponse> create(@AuthenticationPrincipal Long userId,
                                                     @RequestBody CreateRoomRequest request) {
        Long roomId = roomService.create(userId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateRoomResponse(roomId));
    }
}
