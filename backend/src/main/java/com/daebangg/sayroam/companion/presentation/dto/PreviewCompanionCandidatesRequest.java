package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.PreviewCompanionCandidatesQuery;
import com.daebangg.sayroam.companion.application.service.CompanionService;
import com.daebangg.sayroam.companion.domain.CompanionCandidateSort;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import com.daebangg.sayroam.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Map;

public record PreviewCompanionCandidatesRequest(
        @NotNull Long destinationId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        Gender preferredGender,
        @Schema(minimum = "0") Integer preferredMinAge,
        @Schema(minimum = "0") Integer preferredMaxAge,
        @Schema(description = "등록 제목에 포함된 검색어. 앞뒤 공백은 제외", maxLength = CompanionSearch.MAX_TITLE_LENGTH)
        String q,
        @Schema(description = "0부터 시작하는 페이지", minimum = "0", defaultValue = "0")
        Integer page,
        @Schema(description = "페이지당 후보 수", minimum = "1", defaultValue = "20",
                maximum = "" + CompanionService.MAX_CANDIDATE_PAGE_SIZE)
        Integer size,
        @Schema(description = "OVERLAP는 겹치는 일수순, STYLE은 여행 스타일 점수순. STYLE은 4문항 응답 필수",
                defaultValue = "OVERLAP")
        CompanionCandidateSort sort,
        @Schema(description = "등록 전 4문항 점수. 하나라도 전달하면 네 점수 모두 필요하며 프로필 대신 사용",
                minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE)
        Integer planning,
        @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE)
        Integer dailyPace,
        @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE)
        Integer timeTogether,
        @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE)
        Integer spending
) {
    public PreviewCompanionCandidatesQuery toQuery() {
        return new PreviewCompanionCandidatesQuery(destinationId, startDate, endDate, preferredGender,
                preferredMinAge, preferredMaxAge, q, travelStyleAnswers(),
                page == null ? 0 : page, size == null ? 20 : size,
                sort == null ? CompanionCandidateSort.OVERLAP : sort);
    }

    private @Nullable Map<String, Integer> travelStyleAnswers() {
        if (planning == null && dailyPace == null && timeTogether == null && spending == null) {
            return null;
        }
        return new RecruitmentTravelStyleRequest(planning, dailyPace, timeTogether, spending).answers();
    }
}
