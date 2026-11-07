package com.daebangg.sayroam.user.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.user.application.service.UserService;
import com.daebangg.sayroam.user.presentation.api.UserApi;
import com.daebangg.sayroam.user.presentation.dto.CurrentUserResponse;
import com.daebangg.sayroam.user.presentation.dto.UpdateUserProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CurrentUserController implements UserApi {
    private final UserService userService;

    @Override
    @GetMapping(value = "/api/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal AuthenticatedAccount account) {
        var profile = userService.getCurrentUser(account.userId(), account.authVersion());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(CurrentUserResponse.from(profile));
    }

    @Override
    @PatchMapping(value = "/api/users/me", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentUserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestBody UpdateUserProfileRequest request) {
        var profile = userService.updateProfile(account.userId(), account.authVersion(), request.toCommand());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(CurrentUserResponse.from(profile));
    }
}
