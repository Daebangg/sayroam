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
        @Nullable String title,
        @Schema(description = "동행 소개글. 앞뒤 공백 제거 후 최대 200자. 수정 시 생략 또는 null이면 유지, 공백만 입력하면 삭제",
                types = {"string", "null"}, maxLength = CompanionSearch.MAX_INTRODUCTION_LENGTH)
        @Nullable String introduction,
        @Schema(description = "내 만 나이 공개 여부. 생략 또는 null이면 신규 등록은 비공개, 수정은 기존 설정 유지",
                types = {"boolean", "null"})
        @Nullable Boolean showAge,
        @Schema(description = "내 성별 공개 여부. 생략 또는 null이면 신규 등록은 비공개, 수정은 기존 설정 유지",
                types = {"boolean", "null"})
        @Nullable Boolean showGender,
        @Schema(description = "모집에만 저장할 4문항 점수(1~5). 신규 등록에서 생략하면 프로필을 복사하고 "
                + "수정에서 생략하면 모집 저장값을 유지합니다.", nullable = true)
        @Nullable RecruitmentTravelStyleRequest travelStyle
) {

    public SaveCompanionSearchCommand toCommand() {
        return new SaveCompanionSearchCommand(destinationId, startDate, endDate, preferredGender,
                preferredMinAge, preferredMaxAge, title, introduction, showAge, showGender,
                travelStyle == null ? null : travelStyle.answers());
    }
}
