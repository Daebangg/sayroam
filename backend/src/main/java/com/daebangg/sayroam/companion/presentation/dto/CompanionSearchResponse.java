package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.companion.domain.CompanionMatchingStatus;
import com.daebangg.sayroam.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Map;

@Schema(requiredProperties = {"id", "destinationId", "legacyRegion", "startDate", "endDate",
        "preferredGender", "preferredMinAge", "preferredMaxAge", "status", "title", "introduction", "showAge", "showGender", "travelStyle"})
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
        boolean showGender,
        @Schema(description = "모집에 저장된 4문항 점수. 기존 미설정 모집은 빈 객체이며 수정으로 보완해야 합니다.")
        Map<String, Integer> travelStyle,
        @Schema(description = "현재 모집 조건 버전") long matchingRevision,
        @Schema(description = "현재 조건의 최초 탐색 상태. 후보 목록은 별도 조회합니다.", enumAsRef = true)
        CompanionMatchingStatus matchingStatus
) {

    public static CompanionSearchResponse from(CompanionSearchResult result) {
        return new CompanionSearchResponse(result.id(), result.destinationId(), result.legacyRegion(),
                result.startDate(), result.endDate(), result.preferredGender(),
                result.preferredMinAge(), result.preferredMaxAge(), result.status(), result.title(),
                result.introduction(), result.showAge(), result.showGender(), result.travelStyle(),
                result.matchingRevision(), result.matchingStatus());
    }
}
