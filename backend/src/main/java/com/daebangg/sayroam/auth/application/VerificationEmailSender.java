package com.daebangg.sayroam.auth.application;

public interface VerificationEmailSender {
    void sendVerificationCode(String recipient, String code);
}
