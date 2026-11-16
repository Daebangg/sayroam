package com.daebangg.sayroam.companion.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.presentation.dto.ChangeCompanionSearchStatusRequest;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchResponse;
import com.daebangg.sayroam.companion.presentation.dto.SaveCompanionSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CompanionSearch")
@SecurityRequirement(name = "bearerAuth")
public interface CompanionSearchApi {
    @Operation(operationId = "createCompanionSearch", summary = "동행 매칭 등록",
            description = "활성 여행지와 여행 기간을 저장합니다. 여행 스타일은 기존 프로필 응답을 사용합니다.")
    @ApiResponse(responseCode = "201", description = "등록 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchResponse> create(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   SaveCompanionSearchRequest request);

    @Operation(operationId = "listMyCompanionSearches", summary = "본인의 동행 매칭 등록 목록",
            description = "종료한 등록을 포함해 최신 등록순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "본인 등록 목록")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    ResponseEntity<CompanionSearchListResponse> findMine(@Parameter(hidden = true) AuthenticatedAccount account);

    @Operation(operationId = "updateCompanionSearch", summary = "동행 매칭 조건 수정",
            description = "활성 등록의 조건을 교체합니다. 생략한 희망 성별과 나이 조건은 해제합니다.")
    @ApiResponse(responseCode = "200", description = "수정 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionSearchClosed")
    ResponseEntity<CompanionSearchResponse> update(@Parameter(hidden = true) AuthenticatedAccount account,
                                                   long searchId, SaveCompanionSearchRequest request);

    @Operation(operationId = "closeCompanionSearch", summary = "동행 매칭 종료",
            description = "상태를 CLOSED로 변경합니다. 이미 종료한 등록에도 같은 요청을 보낼 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "종료 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    ResponseEntity<CompanionSearchResponse> changeStatus(@Parameter(hidden = true) AuthenticatedAccount account,
                                                         long searchId, ChangeCompanionSearchStatusRequest request);
}
