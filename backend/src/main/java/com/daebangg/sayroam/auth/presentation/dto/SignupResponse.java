package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupResponse(@Schema(description = "생성된 회원 ID. 가입은 자동 로그인을 수행하지 않음") long userId) {
}
