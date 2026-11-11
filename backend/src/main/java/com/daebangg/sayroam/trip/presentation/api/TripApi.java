package com.daebangg.sayroam.trip.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripSummaryResponse;
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

    @Operation(operationId = "createTripRoom", summary = "여행에 채팅방 만들어 연결",
            description = "방이 없는 여행에 채팅방을 만들고, 요청한 회원을 방장으로 등록한 뒤 여행에 연결합니다. "
                    + "여행을 만든 회원만 사용할 수 있습니다. 이미 방이 연결된 여행은 여행 조회 응답의 roomId로 초대 링크를 발급합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "방 생성, 방장 등록, 여행 연결 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/RoomBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/TripRoomAlreadyLinked"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CreateTripRoomResponse> createRoom(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                                      @Parameter(description = "여행 ID", example = "12") Long tripId,
                                                      @RequestBody CreateTripRoomRequest request);

    @Operation(operationId = "listMyTrips", summary = "내 여행 목록 조회",
            description = "로그인한 회원이 만든 여행을 나중에 만든 여행부터 조회합니다. 지난 여행도 포함합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 목록. 만든 여행이 없으면 빈 배열"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TripListResponse> findMine(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account);

    @Operation(operationId = "getTrip", summary = "여행 조회",
            description = "로그인한 회원이 만든 여행 하나를 조회합니다. 다른 회원의 여행은 없는 여행과 같이 404로 응답합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 조회 완료"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TripSummaryResponse> find(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                             @Parameter(description = "여행 ID", example = "12") Long tripId);
}
