package com.daebangg.sayroam.global.exception;

public interface ErrorCode {
    ErrorType getType();

    String getCode();

    String getMessage();

}
