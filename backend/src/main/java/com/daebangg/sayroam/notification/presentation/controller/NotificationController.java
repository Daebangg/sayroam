package com.daebangg.sayroam.notification.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.notification.application.dto.NotificationListQuery;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.presentation.api.NotificationApi;
import com.daebangg.sayroam.notification.presentation.dto.NotificationListResponse;
import com.daebangg.sayroam.notification.presentation.dto.NotificationSummaryResponse;
import com.daebangg.sayroam.notification.presentation.dto.ReadAllNotificationsRequest;
import com.daebangg.sayroam.notification.presentation.dto.UpdateNotificationReadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {
    private final NotificationService notifications;

    @Override
    @GetMapping
    public ResponseEntity<NotificationListResponse> findAll(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        NotificationListQuery query = new NotificationListQuery(account.userId(), beforeId, size, unreadOnly);
        return ResponseEntity.ok(NotificationListResponse.from(notifications.findAll(query)));
    }

    @Override
    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthenticatedAccount account) {
        return ResponseEntity.ok(NotificationSummaryResponse.from(notifications.getSummary(account.userId())));
    }

    @Override
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> readAll(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestBody ReadAllNotificationsRequest request) {
        notifications.readAll(account.userId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping(value = "/{notificationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateReadStatus(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long notificationId,
            @RequestBody UpdateNotificationReadRequest request) {
        if (request.isRead()) {
            notifications.read(account.userId(), notificationId);
        } else {
            notifications.unread(account.userId(), notificationId);
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long notificationId) {
        notifications.delete(account.userId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
