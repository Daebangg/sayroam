package com.daebangg.sayroam.user.presentation.dto;

import com.daebangg.sayroam.user.application.result.UserLoginMethodsResult;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UserLoginMethodsResponse(
        @Schema(description = "비밀번호 로그인 수단이 등록되어 있는지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean passwordEnabled,
        @Schema(description = "현재 회원에게 실제로 연결된 소셜 로그인 공급자. 연결이 없으면 빈 배열", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SocialProvider> socialProviders) {
    public static UserLoginMethodsResponse from(UserLoginMethodsResult result) {
        return new UserLoginMethodsResponse(result.passwordEnabled(), result.socialProviders());
    }
}
