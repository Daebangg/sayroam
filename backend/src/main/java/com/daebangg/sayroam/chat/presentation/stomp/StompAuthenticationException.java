package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.global.exception.CommonErrorCode;

public class StompAuthenticationException extends RuntimeException {
    public StompAuthenticationException() {
        super(CommonErrorCode.UNAUTHENTICATED.getMessage());
    }
}
