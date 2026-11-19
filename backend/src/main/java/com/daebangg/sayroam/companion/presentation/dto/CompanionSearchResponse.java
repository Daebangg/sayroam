package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Schema(requiredProperties = {"id", "destinationId", "legacyRegion", "startDate", "endDate",
        "preferredGender", "preferredMinAge", "preferredMaxAge", "status", "title", "introduction", "showAge", "showGender"})
public record CompanionSearchResponse(
        Long id,
        @Schema(description = "기존 자유 입력 등록이면 null", types = {"integer", "null"}, format = "int64")
        Long destinationId,
        @Schema(description = "기존 자유 입력 등록의 지역명", types = {"string", "null"}) String legacyRegion,
        LocalDate startDate,
        LocalDate endDate,
        @Schema(enumAsRef = true) @Nullable Gender preferredGender,
        @Schema(types = {"integer", "null"}, format = "int32") Integer preferredMinAge,
        @Schema(types = {"integer", "null"}, format = "int32") Integer preferredMaxAge,
        CompanionSearchStatus status,
        @Schema(types = {"string", "null"}) String title,
        @Schema(types = {"string", "null"}) @Nullable String introduction,
        boolean showAge,
        boolean showGender
) {

    public static CompanionSearchResponse from(CompanionSearchResult result) {
        return new CompanionSearchResponse(result.id(), result.destinationId(), result.legacyRegion(),
                result.startDate(), result.endDate(), result.preferredGender(),
                result.preferredMinAge(), result.preferredMaxAge(), result.status(), result.title(),
                result.introduction(), result.showAge(), result.showGender());
    }
}
