package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.exception.ErrorCode;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload,
                                             Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
        errorHeaderAccessor.setMessage(errorCodeOf(cause).getCode());
        return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
    }

    private ErrorCode errorCodeOf(Throwable cause) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(cause);
        if (rootCause instanceof StompAuthenticationException) {
            return CommonErrorCode.UNAUTHENTICATED;
        }
        if (rootCause instanceof StompAccessDeniedException) {
            return CommonErrorCode.ACCESS_DENIED;
        }
        if (rootCause instanceof BusinessException businessException) {
            return businessException.getErrorCode();
        }
        return CommonErrorCode.INTERNAL_SERVER_ERROR;
    }
}
