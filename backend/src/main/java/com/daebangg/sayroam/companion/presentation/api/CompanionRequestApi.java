package com.daebangg.sayroam.companion.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.presentation.dto.CompanionRequestResponse;
import com.daebangg.sayroam.companion.presentation.dto.RespondCompanionRequest;
import com.daebangg.sayroam.companion.presentation.dto.SendCompanionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "CompanionRequest")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "200", description = "처리 결과")
@ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
@ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
public interface CompanionRequestApi {
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionCandidateUnavailable")
    @Operation(operationId = "sendCompanionRequest", summary = "두 모집 사이의 롬 메이트 요청 생성 또는 기존 요청 확인")
    CompanionRequestResponse send(@Parameter(hidden = true) AuthenticatedAccount account, long searchId, SendCompanionRequest body);

    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionRequestNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/CompanionRequestAlreadyAnswered")
    @Operation(operationId = "respondToCompanionRequest", summary = "받은 요청 수락 또는 거절",
            description = "이미 만료된 요청에는 EXPIRED를 반환합니다. 같은 응답은 반복해도 결과가 유지됩니다.")
    CompanionRequestResponse respond(@Parameter(hidden = true) AuthenticatedAccount account, long requestId, RespondCompanionRequest body);

    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionRequestNotFound")
    @Operation(operationId = "getCompanionRequest", summary = "당사자의 롬 메이트 요청 상세")
    CompanionRequestResponse find(@Parameter(hidden = true) AuthenticatedAccount account, long requestId);

    @ApiResponse(responseCode = "404", ref = "#/components/responses/CompanionSearchNotFound")
    @Operation(operationId = "listCompanionRequests", summary = "내 모집의 보낸·받은 요청 조회",
            description = "최신 요청 순서로 반환합니다. 거절된 매칭 목록은 status=REJECTED로 조회합니다.")
    List<CompanionRequestResponse> list(@Parameter(hidden = true) AuthenticatedAccount account, long searchId, CompanionRequestStatus status);
}
