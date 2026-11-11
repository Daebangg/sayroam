package com.daebangg.sayroam.chat.application.port.out;

import com.daebangg.sayroam.global.exception.ErrorCode;

// 서버가 클라이언트의 채팅 연결을 끊는다. 클라이언트에게 오류 코드를 알린 뒤 연결을 닫는다.
public interface ChatConnectionCloser {

    void close(String sessionId, ErrorCode errorCode);
}
