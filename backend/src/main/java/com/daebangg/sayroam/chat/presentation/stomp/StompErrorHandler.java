package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.exception.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload,
                                             Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
        // 서버가 직접 보낸 ERROR 프레임은 원인 예외가 없고 코드가 이미 담겨 있다.
        if (cause != null) {
            errorHeaderAccessor.setMessage(errorCodeOf(cause).getCode());
        }
        return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
    }

    private ErrorCode errorCodeOf(Throwable cause) {
        // 인프라 예외가 원인으로 이어질 수 있어 가장 안쪽이 아니라 사슬에서 우리 예외를 찾는다.
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof StompAuthenticationException) {
                return CommonErrorCode.UNAUTHENTICATED;
            }
            if (current instanceof StompAccessDeniedException) {
                return CommonErrorCode.ACCESS_DENIED;
            }
            if (current instanceof BusinessException businessException) {
                return businessException.getErrorCode();
            }
        }
        return CommonErrorCode.INTERNAL_SERVER_ERROR;
    }
}
