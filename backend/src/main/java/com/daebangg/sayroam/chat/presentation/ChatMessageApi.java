package com.daebangg.sayroam.chat.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.chat.presentation.dto.ChatMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "Chat")
public interface ChatMessageApi {
    @Operation(summary = "놓친 메시지 조회",
            description = """
                    afterId 이후 메시지를 ID 오름차순으로 조회합니다. 입장 이후 메시지만 조회됩니다.
                    응답 개수가 size와 같으면 마지막 messageId를 afterId로 다시 요청합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 메시지가 없으면 빈 배열"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/RoomAccessDenied"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<List<ChatMessageResponse>> findAfter(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Parameter(description = "방 ID", example = "7") Long roomId,
            @Parameter(description = "마지막으로 받은 메시지 ID. 생략하면 처음부터 조회",
                    schema = @Schema(defaultValue = "0", example = "100")) Long afterId,
            @Parameter(description = "한 번에 조회할 최대 개수",
                    schema = @Schema(minimum = "1", maximum = "100", defaultValue = "100"))
            @Min(1) @Max(100) int size);
}

