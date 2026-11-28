package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.FindCompanionCandidatesQuery;
import com.daebangg.sayroam.companion.application.service.CompanionService;
import com.daebangg.sayroam.companion.domain.CompanionCandidateSort;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import io.swagger.v3.oas.annotations.media.Schema;

public record FindCompanionCandidatesRequest(
        @Schema(description = "등록 제목에 포함된 검색어. 앞뒤 공백은 제외", maxLength = CompanionSearch.MAX_TITLE_LENGTH)
        String q,
        @Schema(description = "0부터 시작하는 페이지", minimum = "0", defaultValue = "0")
        Integer page,
        @Schema(description = "페이지당 후보 수", minimum = "1", defaultValue = "20",
                maximum = "" + CompanionService.MAX_CANDIDATE_PAGE_SIZE)
        Integer size,
        @Schema(description = "OVERLAP는 겹치는 일수순, STYLE은 여행 스타일 점수순. STYLE은 4문항 응답 필수",
                defaultValue = "OVERLAP")
        CompanionCandidateSort sort
) {
    public FindCompanionCandidatesQuery toQuery() {
        return new FindCompanionCandidatesQuery(q, page == null ? 0 : page, size == null ? 20 : size,
                sort == null ? CompanionCandidateSort.OVERLAP : sort);
    }
}
