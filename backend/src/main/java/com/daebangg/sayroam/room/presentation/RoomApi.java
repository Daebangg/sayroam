package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomRequest;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
}
