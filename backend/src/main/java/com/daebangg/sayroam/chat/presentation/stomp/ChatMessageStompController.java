package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.service.AccountAuthenticationService;
import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.application.port.out.ChatConnectionCloser;
import com.daebangg.sayroam.chat.application.service.ChatMessageService;
import com.daebangg.sayroam.chat.presentation.dto.ChatErrorResponse;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageRequest;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageResponse;
import com.daebangg.sayroam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageStompController {
    private final ChatMessageService chatMessageService;
    private final AccountAuthenticationService accountAuthenticationService;
    private final ChatConnectionCloser sessionCloser;

    @MessageMapping("/rooms/{roomId}/messages")
    @SendToUser("/queue/messages")
    public ChatMessageResponse send(@DestinationVariable Long roomId, ChatPrincipal principal,
                                    @Payload ChatMessageRequest request) {
        accountAuthenticationService.ensureActive(principal.userId(), principal.authVersion());
        ChatMessageResult result = chatMessageService.send(roomId, principal.userId(), request.clientMessageId(), request.content());
        return ChatMessageResponse.from(result);
    }

    // 저장소 장애는 재시도할 수 있으니 연결을 유지하고, 무효화된 토큰은 연결을 끊는다.
    @MessageExceptionHandler(AuthException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorResponse handleAuth(AuthException e, @Payload ChatMessageRequest request,
                                        @Header(SimpMessageHeaderAccessor.SESSION_ID_HEADER) String sessionId) {
        if (e.getErrorCode() != AuthErrorCode.AUTHENTICATION_UNAVAILABLE) {
            sessionCloser.close(sessionId, e.getErrorCode());
            return null;
        }
        return new ChatErrorResponse(request.clientMessageId(), e.getErrorCode().getCode(), e.getMessage());
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorResponse handle(BusinessException e, @Payload ChatMessageRequest request) {
        return new ChatErrorResponse(request.clientMessageId(), e.getErrorCode().getCode(), e.getMessage());
    }
}
