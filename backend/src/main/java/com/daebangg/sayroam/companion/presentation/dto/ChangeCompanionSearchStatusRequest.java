package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeCompanionSearchStatusRequest(
        @NotNull @Schema(description = "ACTIVE는 모집 재개, CLOSED는 모집 종료. CONFIRMED는 요청 수락으로만 전환되며 "
                + "직접 지정할 수 없습니다. 동행 확정된 모집의 상태는 변경할 수 없습니다.", enumAsRef = true)
        CompanionSearchStatus status
) {
}
