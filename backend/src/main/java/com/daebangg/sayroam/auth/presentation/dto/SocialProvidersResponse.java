package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.user.domain.type.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SocialProvidersResponse(
        @Schema(description = "서버에 활성화된 공급자만 반환. 클라이언트 ID·비밀값은 포함하지 않음", requiredMode = Schema.RequiredMode.REQUIRED)
        List<SocialProvider> providers) { }
