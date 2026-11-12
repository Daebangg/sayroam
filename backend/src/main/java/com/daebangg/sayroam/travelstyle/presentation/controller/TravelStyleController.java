package com.daebangg.sayroam.travelstyle.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.travelstyle.application.service.TravelStyleService;
import com.daebangg.sayroam.travelstyle.presentation.api.TravelStyleApi;
import com.daebangg.sayroam.travelstyle.presentation.dto.TravelStyleResponse;
import com.daebangg.sayroam.travelstyle.presentation.dto.UpdateTravelStyleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/users/me/travel-style", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TravelStyleController implements TravelStyleApi {
    private final TravelStyleService service;

    @Override
    @GetMapping
    public ResponseEntity<TravelStyleResponse> findMine(@AuthenticationPrincipal AuthenticatedAccount account) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(TravelStyleResponse.from(service.findMine(account.userId(), account.authVersion())));
    }

    @Override
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TravelStyleResponse> updateMine(@AuthenticationPrincipal AuthenticatedAccount account,
                                                         @RequestBody UpdateTravelStyleRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(TravelStyleResponse.from(service.updateMine(account.userId(), account.authVersion(),
                        request.toCommand())));
    }
}
