package com.daebangg.sayroam.companion.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.presentation.dto.ChangeCompanionSearchStatusRequest;
import com.daebangg.sayroam.companion.presentation.dto.CompanionCandidateListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchResponse;
import com.daebangg.sayroam.companion.presentation.dto.SaveCompanionSearchRequest;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CompanionSearch")
@SecurityRequirement(name = "bearerAuth")
public interface CompanionSearchApi {
    @Operation(operationId = "createCompanionSearch", summary = "동행 매칭 등록",
            description = "활성 여행지와 여행 기간, 선택한 상대 조건, 제목, 소개글과 내 정보 공개 설정을 저장합니다. "
                    + "회원당 활성 모집은 최대 3개입니다. 종료한 모집은 개수에서 제외합니다.")
    @ApiResponse(responseCode = "201", description = "등록 완료")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchLimitExceeded")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchResponse> create(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   SaveCompanionSearchRequest request);

    @Operation(operationId = "listMyCompanionSearches", summary = "본인의 동행 매칭 등록 목록",
            description = "종료한 등록을 포함해 최신 등록순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "본인 등록 목록")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchListResponse> findMine(@Parameter(hidden = true) AuthenticatedAccount account);

    @Operation(operationId = "listCompanionCandidates", summary = "동행 매칭 후보 조회",
            description = "등록한 두 여행 기간이 하루 이상 겹치고 양쪽의 희망 나이·성별 조건을 충족하는 후보를 조회합니다. "
                    + "여행 기간의 시작일과 종료일을 포함해 겹치는 일수가 많은 순으로, 회원별 한 건을 반환합니다. "
                    + "만 나이와 성별은 해당 등록에서 공개한 값만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "후보 목록. 조건에 맞는 후보가 없으면 빈 배열")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionCandidateSearchUnavailable")
    ResponseEntity<CompanionCandidateListResponse> findCandidates(
            @Parameter(hidden = true) AuthenticatedAccount account,
            long searchId,
            @Parameter(description = "등록 제목에 포함된 검색어. 앞뒤 공백은 제외",
                    schema = @Schema(type = "string", maxLength = CompanionSearch.MAX_TITLE_LENGTH)) String q,
            @Parameter(description = "0부터 시작하는 페이지",
                    schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0")) int page,
            @Parameter(description = "페이지당 후보 수",
                    schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "20",
                            maximum = "" + CompanionSearchService.MAX_CANDIDATE_PAGE_SIZE)) int size);

    @Operation(operationId = "updateCompanionSearch", summary = "동행 매칭 조건 수정",
            description = "활성 등록의 조건을 교체합니다. 희망 성별과 나이 조건, 제목은 생략하면 해제합니다. "
                    + "소개글과 내 정보 공개 설정은 생략하거나 null이면 유지합니다.")
    @ApiResponse(responseCode = "200", description = "수정 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchClosed")
    ResponseEntity<CompanionSearchResponse> update(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   long searchId, SaveCompanionSearchRequest request);

    @Operation(operationId = "closeCompanionSearch", summary = "동행 모집 종료 또는 재개",
            description = "CLOSED는 모집 종료, ACTIVE는 모집 재개입니다. 이미 같은 상태인 등록에도 요청할 수 있습니다. "
                    + "재개하려면 활성 여행지가 필요하며, 활성 모집이 3개 미만이어야 합니다. "
                    + "이미 ACTIVE인 등록에 같은 요청을 반복하면 한도와 무관하게 성공합니다.")
    @ApiResponse(responseCode = "200", description = "상태 변경 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchStatusConflict")
    ResponseEntity<CompanionSearchResponse> changeStatus(@Parameter(hidden = true) AuthenticatedAccount account,
                                                         long searchId, ChangeCompanionSearchStatusRequest request);
}
