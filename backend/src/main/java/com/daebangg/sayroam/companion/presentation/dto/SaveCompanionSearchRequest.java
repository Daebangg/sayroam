package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
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
        @PositiveOrZero Integer preferredMaxAge,
        @Schema(description = "동행 제목. 앞뒤 공백 제거 후 최대 80자. 생략하거나 비어 있으면 제목 없음",
                types = {"string", "null"}, maxLength = CompanionSearch.MAX_TITLE_LENGTH)
        @Nullable String title
) {

    public SaveCompanionSearchCommand toCommand() {
        return new SaveCompanionSearchCommand(destinationId, startDate, endDate, preferredGender,
                preferredMinAge, preferredMaxAge, title);
    }
}
