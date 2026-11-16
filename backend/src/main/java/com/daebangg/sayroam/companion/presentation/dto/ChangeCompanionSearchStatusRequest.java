package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeCompanionSearchStatusRequest(
        @NotNull @Schema(allowableValues = "CLOSED") CompanionSearchStatus status
) {
}
