package com.daebangg.sayroam.companion.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.presentation.dto.CompanionCandidateListResponse;
import com.daebangg.sayroam.user.domain.Gender;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Tag(name = "CompanionSearch")
@SecurityRequirement(name = "bearerAuth")
public interface CompanionCandidatePreviewApi {

    @Operation(operationId = "previewCompanionCandidates", summary = "동행 등록 전 후보 조회",
            description = "여행지와 기간, 선택한 상대 조건으로 동행 후보를 미리 조회합니다. "
                    + "상대의 희망 조건은 로그인 회원의 프로필과 비교합니다. "
                    + "만 나이와 성별은 해당 등록에서 공개한 값만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "후보 목록. 조건에 맞는 후보가 없으면 빈 배열")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionCandidateListResponse> preview(
            @Parameter(hidden = true) AuthenticatedAccount account,
            long destinationId, LocalDate startDate, LocalDate endDate, Gender preferredGender,
            @Parameter(schema = @Schema(type = "integer", format = "int32", minimum = "0")) Integer preferredMinAge,
            @Parameter(schema = @Schema(type = "integer", format = "int32", minimum = "0")) Integer preferredMaxAge,
            @Parameter(description = "등록 제목에 포함된 검색어. 앞뒤 공백은 제외",
                    schema = @Schema(type = "string", maxLength = CompanionSearch.MAX_TITLE_LENGTH)) String q,
            @Parameter(description = "0부터 시작하는 페이지",
                    schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0")) int page,
            @Parameter(description = "페이지당 후보 수",
                    schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "20",
                            maximum = "" + CompanionSearchService.MAX_CANDIDATE_PAGE_SIZE)) int size);
}
