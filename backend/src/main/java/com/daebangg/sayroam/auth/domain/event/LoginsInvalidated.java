package com.daebangg.sayroam.auth.domain.event;

// 비밀번호 변경·재설정, 탈퇴로 회원의 기존 로그인이 모두 무효화됐다.
public record LoginsInvalidated(long userId) {
}
