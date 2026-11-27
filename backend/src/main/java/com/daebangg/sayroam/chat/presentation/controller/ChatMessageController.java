package com.daebangg.sayroam.chat.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.chat.application.service.ChatMessageService;
import com.daebangg.sayroam.chat.presentation.api.ChatMessageApi;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageResponse;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageSummaryResponse;
import com.daebangg.sayroam.chat.presentation.dto.ReadPositionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatMessageController implements ChatMessageApi {
    private final ChatMessageService chatMessageService;

    @Override
    @GetMapping(value = "/api/rooms/{roomId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatMessageResponse>> findAfter(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "100") int size) {

        List<ChatMessageResponse> messages = chatMessageService.findAfter(roomId, account.userId(), afterId, size)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ResponseEntity.ok(messages);
    }

    @Override
    @PatchMapping(value = "/api/rooms/{roomId}/read-position", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> advanceLastRead(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long roomId,
            @RequestBody ReadPositionRequest request) {
        chatMessageService.advanceLastRead(roomId, account.userId(), request.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping(value = "/api/rooms/{roomId}/messages/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatMessageSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long roomId) {
        return ResponseEntity.ok(ChatMessageSummaryResponse.from(chatMessageService.getSummary(roomId, account.userId())));
    }
}
