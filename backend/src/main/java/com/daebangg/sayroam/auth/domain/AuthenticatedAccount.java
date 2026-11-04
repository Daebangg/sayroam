package com.daebangg.sayroam.auth.domain;

import com.daebangg.sayroam.user.domain.Role;

import java.security.Principal;

/** 인증 시점에 조회한 회원 상태. JPA 엔티티나 비밀번호를 보안 컨텍스트에 보관하지 않는다. */
public record AuthenticatedAccount(long userId, String email, String nickname,
                                   String profileImageUrl, Role role) implements Principal {
    @Override
    public String getName() {
        return Long.toString(userId);
    }

    @Override
    public String toString() {
        return "AuthenticatedAccount[userId=" + userId + "]";
    }
}
