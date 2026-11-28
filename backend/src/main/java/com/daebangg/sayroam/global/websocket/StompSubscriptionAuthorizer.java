package com.daebangg.sayroam.global.websocket;

public interface StompSubscriptionAuthorizer {
    boolean supports(String destination);

    void authorize(String destination, Long userId);
}
