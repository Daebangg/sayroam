package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeCompanionSearchStatusRequest(
        @NotNull @Schema(description = "ACTIVE는 모집 재개, CLOSED는 모집 종료", enumAsRef = true)
        CompanionSearchStatus status
) {
}
