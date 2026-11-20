package com.daebangg.sayroam.notification.presentation.api;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.notification.application.dto.NotificationListQuery;
import com.daebangg.sayroam.notification.presentation.dto.NotificationListResponse;
import com.daebangg.sayroam.notification.presentation.dto.NotificationSummaryResponse;
import com.daebangg.sayroam.notification.presentation.dto.ReadAllNotificationsRequest;
import com.daebangg.sayroam.notification.presentation.dto.UpdateNotificationReadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

@Tag(name = "Notification")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationApi {
    @Operation(operationId = "listMyNotifications", summary = "내 알림 목록 조회",
            description = "로그인한 회원의 삭제되지 않은 알림을 ID 내림차순으로 조회합니다. 조회만으로 읽음 처리하지 않습니다. "
                    + "unreadOnly=true이면 미읽음 알림만 조회합니다. 필터 변경·재접속 시에는 beforeId를 생략하고, "
                    + "다음 조회는 같은 필터와 이전 목록의 마지막 알림 ID를 전달합니다. 알림이 없으면 빈 배열을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "알림 목록 조회 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable")
    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    ResponseEntity<NotificationListResponse> findAll(
            @Parameter(hidden = true) AuthenticatedAccount account,
            @Parameter(description = "이 ID보다 작은 알림 조회. 첫 조회는 생략",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")) Long beforeId,
            @Parameter(description = "조회할 최대 알림 개수",
                    schema = @Schema(type = "integer", format = "int32", minimum = "1",
                            maximum = "" + NotificationListQuery.MAX_PAGE_SIZE, defaultValue = "20")) int size,
            @Parameter(description = "true이면 읽지 않은 알림만 조회", schema = @Schema(type = "boolean", defaultValue = "false"))
            boolean unreadOnly);

    @Operation(operationId = "getMyNotificationSummary", summary = "내 알림 개수 요약 조회",
            description = "로그인한 회원의 삭제되지 않은 전체 알림 개수와 미읽음 개수를 조회합니다. "
                    + "목록 페이지의 개수와 무관한 전체 집계이며 알림이 없으면 두 값 모두 0입니다. 조회만으로 읽음 처리하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "알림 개수 요약 조회 완료")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable")
    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    ResponseEntity<NotificationSummaryResponse> getSummary(@Parameter(hidden = true) AuthenticatedAccount account);

    @Operation(operationId = "readAllMyNotifications", summary = "내 알림 전체 읽음 처리",
            description = "처리 시점에 존재하는 본인의 미삭제·미읽음 알림을 한 번에 읽음 처리합니다. "
                    + "이미 읽은 알림의 최초 읽음 시각과 삭제한 알림은 유지합니다. 알림이 없거나 반복 요청해도 204입니다. "
                    + "이후 도착한 알림은 미읽음으로 남습니다. 본문의 isRead에는 true만 허용합니다.")
    @ApiResponse(responseCode = "204", description = "전체 읽음 처리 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable")
    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    ResponseEntity<Void> readAll(@Parameter(hidden = true) AuthenticatedAccount account,
                                 @Valid ReadAllNotificationsRequest request);

    @Operation(operationId = "updateNotificationReadStatus", summary = "알림 읽음 상태 변경",
            description = "본인의 알림을 읽음 또는 읽지 않음으로 변경합니다. 같은 상태를 반복 요청할 수 있으며, "
                    + "이미 읽은 알림에 다시 읽음을 요청해도 최초 읽음 시각을 유지합니다. 삭제한 알림은 변경할 수 없습니다.")
    @ApiResponse(responseCode = "204", description = "읽음 상태 변경 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotificationNotFound")
    @ApiResponse(responseCode = "409", ref = "#/components/responses/NotificationDeleted")
    @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable")
    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    ResponseEntity<Void> updateReadStatus(
            @Parameter(hidden = true) AuthenticatedAccount account,
            @Positive @Parameter(description = "알림 ID",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")) Long notificationId,
            @Valid UpdateNotificationReadRequest request);

    @Operation(operationId = "deleteNotification", summary = "알림 삭제",
            description = "본인의 알림을 알림함에서 숨깁니다. 반복 삭제는 성공으로 응답하며 최초 삭제 시각을 유지합니다. "
                    + "관련 여행·매칭 등 원래 데이터는 삭제하지 않습니다.")
    @ApiResponse(responseCode = "204", description = "알림 삭제 완료")
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest")
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotificationNotFound")
    @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable")
    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) AuthenticatedAccount account,
            @Positive @Parameter(description = "알림 ID",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")) Long notificationId);
}
