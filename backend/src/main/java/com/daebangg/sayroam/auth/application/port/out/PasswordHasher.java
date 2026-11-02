package com.daebangg.sayroam.auth.application.port.out;

public interface PasswordHasher {
    String hash(String password);
}
