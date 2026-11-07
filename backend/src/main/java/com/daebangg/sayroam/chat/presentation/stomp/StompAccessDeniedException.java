package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.global.exception.CommonErrorCode;

public class StompAccessDeniedException extends RuntimeException {
    public StompAccessDeniedException() {
        super(CommonErrorCode.ACCESS_DENIED.getMessage());
    }
}
