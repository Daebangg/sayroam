package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionMatchingStatusResult;
import com.daebangg.sayroam.companion.domain.CompanionMatchingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"searchId", "matchingRevision", "status"})
public record CompanionMatchingStatusResponse(
        long searchId,
        @Schema(description = "현재 모집 조건 버전. 이전 버전의 완료 응답과 구분합니다.") long matchingRevision,
        @Schema(description = "최초 탐색 상태. SUCCEEDED는 탐색 완료이며 후보 존재나 동행 확정을 의미하지 않습니다. "
                + "NOT_REQUESTED는 현재 조건의 작업 없음, SKIPPED는 종료·만료·기존 지역 등으로 탐색 불가입니다.", enumAsRef = true)
        CompanionMatchingStatus status
) {
    public static CompanionMatchingStatusResponse from(CompanionMatchingStatusResult result) {
        return new CompanionMatchingStatusResponse(result.searchId(), result.matchingRevision(), result.status());
    }
}
