package com.daebangg.sayroam.global.websocket;

import com.daebangg.sayroam.global.exception.CommonErrorCode;

public class StompAuthenticationException extends RuntimeException {
    public StompAuthenticationException() {
        super(CommonErrorCode.UNAUTHENTICATED.getMessage());
    }
}
