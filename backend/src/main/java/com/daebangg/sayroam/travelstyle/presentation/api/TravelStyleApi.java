package com.daebangg.sayroam.travelstyle.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.travelstyle.presentation.dto.TravelStyleResponse;
import com.daebangg.sayroam.travelstyle.presentation.dto.UpdateTravelStyleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Travel Style")
public interface TravelStyleApi {
    @Operation(operationId = "getMyTravelStyle", summary = "내 여행 스타일 조회",
            description = "활성 질문과 본인의 응답을 표시 순서대로 반환합니다. 미응답 점수는 null입니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 스타일 조회 완료"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TravelStyleResponse> findMine(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account);

    @Operation(operationId = "updateMyTravelStyle", summary = "내 여행 스타일 수정",
            description = "전달된 문항에 1~5점으로 답하거나 null로 기존 답변을 지웁니다. 생략한 문항의 응답은 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 스타일 수정 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TravelStyleResponse> updateMine(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            UpdateTravelStyleRequest request);
}
