package com.daebangg.sayroam.auth.infrastructure.security;

import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public SpringPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Override
    public String hash(String password) {
        return encoder.encode(password);
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        // 없는 계정과 비밀번호가 없는 소셜 계정도 같은 비용의 비밀번호 검증을 수행한다.
        String candidateHash = passwordHash == null ? dummyHash : passwordHash;
        try {
            return encoder.matches(password, candidateHash) && passwordHash != null;
        } catch (IllegalArgumentException exception) {
            // 지원하지 않거나 손상된 해시도 계정 존재 여부를 공개하지 않는다.
            encoder.matches(password, dummyHash);
            return false;
        }
    }
}
