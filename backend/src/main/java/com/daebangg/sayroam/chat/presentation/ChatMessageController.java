package com.daebangg.sayroam.chat.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.chat.application.ChatMessageService;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatMessageController implements ChatMessageApi {
    private final ChatMessageService chatMessageService;

    @Override
    @GetMapping("/api/rooms/{roomId}/messages")
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
}
