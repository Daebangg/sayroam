package com.daebangg.sayroam.trip.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.presentation.dto.AddTripCustomPlaceRequest;
import com.daebangg.sayroam.trip.presentation.dto.AddTripPlaceRequest;
import com.daebangg.sayroam.trip.presentation.dto.AddTripPlaceResponse;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomResponse;
import com.daebangg.sayroam.trip.presentation.dto.CurrentTripInvitationResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripInvitationResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripPlaceListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripSummaryResponse;
import com.daebangg.sayroam.trip.presentation.dto.UpdateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.UpdateTripResponse;
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
            description = "로그인한 회원이 방 없이 혼자 가는 여행을 만듭니다. 기간과 선택값인 여행 이름을 받습니다. "
                    + "생성 화면은 고른 목적지로 \"제주 여행\" 같은 기본 이름을 만들어 보냅니다. 이름을 생략하거나 비우면 이름 없이 만들어집니다.",
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

    @Operation(operationId = "updateTrip", summary = "여행 수정",
            description = "여행 이름을 바꿉니다. 바꿀 필드만 보내며, 보내지 않은 필드는 그대로 둡니다. 빈 이름을 보내면 이름 없는 여행이 됩니다. "
                    + "채팅방이 없는 여행은 만든 회원이, 채팅방이 연결된 여행은 그 방에 참여 중인 멤버가 수정할 수 있습니다. "
                    + "수정할 수 없는 여행은 없는 여행과 같이 404로 응답합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 수정 완료. 여행 ID와 저장된 이름을 응답"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/TripUpdateBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<UpdateTripResponse> update(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                              @Parameter(description = "여행 ID", example = "12") Long tripId,
                                              @RequestBody UpdateTripRequest request);

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

    @Operation(operationId = "getTripInvitation", summary = "여행 초대 링크 조회",
            description = "지금 쓸 수 있는 초대 코드를 조회합니다. 발급한 적이 없거나 만료됐으면 code가 null이며, "
                    + "이때 화면은 초대 링크 발급을 요청합니다. 코드를 새로 만들지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 초대 코드 또는 null"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CurrentTripInvitationResponse> findInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "여행 ID", example = "12") Long tripId);

    @Operation(operationId = "issueTripInvitation", summary = "여행 초대 링크 발급",
            description = "친구에게 보낼 초대 코드를 발급합니다. 여행마다 링크는 하나이며, 7일 동안 유효합니다. "
                    + "유효한 코드가 있으면 같은 코드를 돌려주고, 없거나 만료됐으면 새 코드로 바꿔 돌려줍니다. "
                    + "링크를 발급해도 채팅방은 만들지 않습니다. 여행을 볼 수 있는 회원이면 누구나 발급할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유효한 초대 코드"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TripInvitationResponse> issueInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "여행 ID", example = "12") Long tripId);

    @Operation(operationId = "addTripPlace", summary = "여행에 장소 추가",
            description = "장소 검색에서 고른 장소를 여행에 추가합니다. 장소 ID만 받고, 이름·분류·주소·좌표는 서버가 TourAPI에서 받아 저장합니다. "
                    + "이미 저장된 장소는 TourAPI를 다시 조회하지 않습니다. 같은 장소를 한 여행에 여러 번 추가할 수 있습니다. "
                    + "채팅방이 없는 여행은 만든 회원이, 채팅방이 연결된 여행은 그 방에 참여 중인 멤버가 추가할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "여행 장소 추가 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/TripPlaceBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripPlaceNotFound"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/PlaceSearchFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PlaceSearchUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<AddTripPlaceResponse> addPlace(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                                  @Parameter(description = "여행 ID", example = "12") Long tripId,
                                                  @RequestBody AddTripPlaceRequest request);

    @Operation(operationId = "addTripCustomPlace", summary = "지도에서 고른 장소를 여행에 직접 추가",
            description = "장소 검색에 없는 곳을 회원이 지도에서 고른 위치(WGS84 위도·경도)와 직접 입력한 이름으로 여행에 추가합니다. "
                    + "서버는 외부 서비스에서 이름이나 주소를 채우지 않습니다. 직접 추가한 장소는 이 여행에만 담기고 장소 검색 결과에는 나오지 않으며, "
                    + "같은 위치와 이름으로 다시 추가해도 따로 저장됩니다. 추가 권한과 일차·메모 기준은 여행에 장소 추가와 같습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "여행 장소 추가 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/TripCustomPlaceBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<AddTripPlaceResponse> addCustomPlace(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                                        @Parameter(description = "여행 ID", example = "12") Long tripId,
                                                        @RequestBody AddTripCustomPlaceRequest request);

    @Operation(operationId = "listMyTrips", summary = "내 여행 목록 조회",
            description = "로그인한 회원이 볼 수 있는 여행을 나중에 만든 여행부터 조회합니다. "
                    + "채팅방이 없는 여행은 만든 회원이, 채팅방이 연결된 여행은 그 방에 참여 중인 멤버가 볼 수 있습니다. 지난 여행도 포함합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 목록. 볼 수 있는 여행이 없으면 빈 배열"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TripListResponse> findMine(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account);

    @Operation(operationId = "getTrip", summary = "여행 조회",
            description = "로그인한 회원이 볼 수 있는 여행 하나를 조회합니다. "
                    + "채팅방이 없는 여행은 만든 회원이, 채팅방이 연결된 여행은 그 방에 참여 중인 멤버가 볼 수 있습니다. "
                    + "볼 수 없는 여행은 없는 여행과 같이 404로 응답합니다.",
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

    @Operation(operationId = "listTripPlaces", summary = "여행에 담긴 장소 목록 조회",
            description = "여행에 담긴 장소를 한 번에 조회합니다. 장소 검색으로 추가한 장소(TOUR)와 지도에서 직접 추가한 장소(CUSTOM)를 함께 돌려주며, "
                    + "일차 순서대로, 일차 미정 장소는 마지막에, 같은 일차 안에서는 추가한 순서로 정렬합니다. "
                    + "저장된 정보로 응답하고 TourAPI를 다시 호출하지 않습니다. "
                    + "채팅방이 없는 여행은 만든 회원이, 채팅방이 연결된 여행은 그 방에 참여 중인 멤버가 볼 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "여행 장소 목록. 담긴 장소가 없으면 빈 배열"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/TripNotFound"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<TripPlaceListResponse> findPlaces(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                                     @Parameter(description = "여행 ID", example = "12") Long tripId);
}
