package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

public record SaveCompanionSearchRequest(
        @NotNull @Positive Long destinationId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Schema(description = "생략하거나 null이면 성별 무관", enumAsRef = true) @Nullable Gender preferredGender,
        @Schema(description = "생략하거나 null이면 최소 나이 제한 없음", types = {"integer", "null"}, format = "int32")
        @PositiveOrZero Integer preferredMinAge,
        @Schema(description = "생략하거나 null이면 최대 나이 제한 없음", types = {"integer", "null"}, format = "int32")
        @PositiveOrZero Integer preferredMaxAge
) {

    public SaveCompanionSearchCommand toCommand() {
        return new SaveCompanionSearchCommand(destinationId, startDate, endDate, preferredGender,
                preferredMinAge, preferredMaxAge);
    }
}
