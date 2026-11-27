package com.daebangg.sayroam.room.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.room.presentation.dto.ReadPositionRequest;
import com.daebangg.sayroam.room.presentation.dto.RoomMemberListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Room")
public interface RoomApi {
    @Operation(summary = "방 멤버 목록 조회",
            description = """
                    방에 참여 중인 회원이 멤버의 닉네임·프로필 이미지·참여 상태를 조회합니다.
                    나간 멤버도 포함하므로 지난 메시지의 senderId로 보낸 사람을 찾을 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/RoomAccessDenied"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<RoomMemberListResponse> findMembers(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "방 ID", example = "7") Long roomId);

    @Operation(summary = "채팅방 나가기",
            description = "참여 중인 방에서 나갑니다. 멤버 기록은 정산을 위해 남고, 유효한 초대 코드로 다시 참여할 수 있습니다. "
                    + "이미 열린 채팅 구독은 서버가 즉시 끊지 않으므로 클라이언트가 해제해야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "나가기 완료"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/RoomAccessDenied"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<Void> leave(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "방 ID", example = "7") Long roomId);

    @Operation(operationId = "advanceLastRead", summary = "읽은 위치 기록",
            description = """
                    참여 중인 방에서 마지막으로 본 메시지 ID를 기록합니다. 안 읽은 메시지 개수는 이 위치 이후로 셉니다.
                    지금 읽은 위치보다 앞선 값은 무시하므로 늦게 도착한 요청이나 다른 기기의 요청이 위치를 되돌리지 않습니다.
                    같은 요청을 반복해도 204입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "기록 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/RoomAccessDenied"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<Void> advanceLastRead(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "방 ID", example = "7") Long roomId,
            @Valid ReadPositionRequest request);
}
