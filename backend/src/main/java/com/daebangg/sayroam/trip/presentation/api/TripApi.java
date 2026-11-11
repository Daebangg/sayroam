package com.daebangg.sayroam.trip.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Trip")
public interface TripApi {
    @Operation(operationId = "createTrip", summary = "여행 생성",
            description = "로그인한 회원이 방 없이 혼자 가는 여행을 만듭니다. 기간만 입력받으며 제목은 비어 있는 상태로 만들어집니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "여행 생성 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/TripBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CreateTripResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                              @RequestBody CreateTripRequest request);
}
