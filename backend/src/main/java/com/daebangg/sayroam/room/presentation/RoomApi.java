package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomInvitationResponse;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomRequest;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomResponse;
import com.daebangg.sayroam.room.presentation.dto.JoinRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Room")
public interface RoomApi {
    @Operation(summary = "채팅방 생성", description = "로그인한 회원을 방장으로 하여 방을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "방과 방장 멤버 생성 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/RoomBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CreateRoomResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
                                              @RequestBody CreateRoomRequest request);

    @Operation(summary = "초대 링크 생성", description = "방 멤버가 7일 동안 유효한 초대 코드를 발급받습니다. 하나의 코드로 여러 명이 참여할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "초대 코드 발급 완료"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/RoomAccessDenied"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<CreateRoomInvitationResponse> createInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "방 ID", example = "7") Long roomId);

    @Operation(summary = "초대 링크로 방 참여", description = "유효한 초대 코드로 방에 참여합니다. 이미 참여 중인 회원은 오류 없이 방 ID를 응답합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여 완료 또는 이미 참여 중"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", description = "ROOM_INVITATION_NOT_FOUND. 없는 초대 코드",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(ref = "#/components/schemas/ApiProblem"))),
            @ApiResponse(responseCode = "410", description = "ROOM_INVITATION_EXPIRED. 만료된 초대 코드",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(ref = "#/components/schemas/ApiProblem"))),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<JoinRoomResponse> joinByInvitation(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "초대 코드", example = "k3Xv9qLm2Tz8RwYb7NfA") String code);
}
