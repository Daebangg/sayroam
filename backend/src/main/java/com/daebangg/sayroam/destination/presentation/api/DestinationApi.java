package com.daebangg.sayroam.destination.presentation.api;

import com.daebangg.sayroam.destination.application.service.DestinationService;
import com.daebangg.sayroam.destination.presentation.dto.DestinationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Destination")
public interface DestinationApi {
    @Operation(operationId = "listDestinations", summary = "국내 매칭 여행지 목록·검색",
            description = "선택 가능한 국내 여행지를 이름순으로 조회합니다. 검색어가 있으면 공백을 제외하고 이름의 일부가 일치하는 여행지를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행지 목록. 검색 결과가 없으면 빈 배열"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<DestinationListResponse> list(
            @Parameter(description = "여행지 이름", example = "고성",
                    schema = @Schema(maxLength = DestinationService.MAX_QUERY_LENGTH))
            @Nullable String query);
}
