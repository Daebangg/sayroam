package com.daebangg.sayroam.room.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.presentation.api.RoomApi;
import com.daebangg.sayroam.room.presentation.dto.ReadPositionRequest;
import com.daebangg.sayroam.room.presentation.dto.RoomMemberListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomApi {
    private final RoomService roomService;

    @Override
    @GetMapping(value = "/api/rooms/{roomId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RoomMemberListResponse> findMembers(@AuthenticationPrincipal AuthenticatedAccount account,
                                                              @PathVariable Long roomId) {
        return ResponseEntity.ok(RoomMemberListResponse.from(roomService.findMembers(roomId, account.userId())));
    }

    @Override
    @DeleteMapping("/api/rooms/{roomId}/members/me")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal AuthenticatedAccount account,
                                      @PathVariable Long roomId) {
        roomService.leave(roomId, account.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping(value = "/api/rooms/{roomId}/members/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> advanceLastRead(@AuthenticationPrincipal AuthenticatedAccount account,
                                                @PathVariable Long roomId,
                                                @RequestBody ReadPositionRequest request) {
        roomService.advanceLastRead(roomId, account.userId(), request.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
