package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.room.presentation.dto.CreateRoomRequest;
import com.daebangg.sayroam.room.presentation.dto.CreateRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Room")
public interface RoomApi {
    @Operation(summary = "채팅방 생성", description = "로그인한 회원을 방장으로 하여 방을 생성합니다. " +
            "이름은 선택값이며 앞뒤 공백을 제거한 뒤 20자 이내여야 합니다. " +
            "공백뿐인 이름은 이름 없는 방으로 저장합니다. CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "방과 방장 멤버 생성 완료"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/RoomBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "XSRF-TOKEN 쿠키에서 복사한 원문 토큰")
    ResponseEntity<CreateRoomResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                              @RequestBody CreateRoomRequest request);
}
