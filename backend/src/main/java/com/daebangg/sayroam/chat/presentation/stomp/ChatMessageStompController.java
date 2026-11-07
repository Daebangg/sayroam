package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.chat.application.ChatMessageService;
import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.presentation.dto.ChatErrorResponse;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageRequest;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageResponse;
import com.daebangg.sayroam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageStompController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/rooms/{roomId}/messages")
    @SendToUser("/queue/messages")
    public ChatMessageResponse send(@DestinationVariable Long roomId, ChatPrincipal principal,
                                    @Payload ChatMessageRequest request) {
        ChatMessageResult result = chatMessageService.send(roomId, principal.userId(), request.clientMessageId(), request.content());
        return ChatMessageResponse.from(result);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorResponse handle(BusinessException e, @Payload ChatMessageRequest request) {
        return new ChatErrorResponse(request.clientMessageId(), e.getErrorCode().getCode(), e.getMessage());
    }
}
