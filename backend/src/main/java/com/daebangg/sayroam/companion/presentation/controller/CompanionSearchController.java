package com.daebangg.sayroam.companion.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.presentation.api.CompanionSearchApi;
import com.daebangg.sayroam.companion.presentation.dto.ChangeCompanionSearchStatusRequest;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchListResponse;
import com.daebangg.sayroam.companion.presentation.dto.CompanionSearchResponse;
import com.daebangg.sayroam.companion.presentation.dto.SaveCompanionSearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/companion-searches", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CompanionSearchController implements CompanionSearchApi {
    private final CompanionSearchService service;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> create(@AuthenticationPrincipal AuthenticatedAccount account,
                                                          @Valid @RequestBody SaveCompanionSearchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CompanionSearchResponse.from(service.create(account.userId(), request.toCommand())));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<CompanionSearchListResponse> findMine(@AuthenticationPrincipal AuthenticatedAccount account) {
        return ResponseEntity.ok(new CompanionSearchListResponse(service.findMine(account.userId()).stream()
                .map(CompanionSearchResponse::from).toList()));
    }

    @Override
    @PutMapping(value = "/{searchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> update(@AuthenticationPrincipal AuthenticatedAccount account,
                                                          @PathVariable long searchId,
                                                          @Valid @RequestBody SaveCompanionSearchRequest request) {
        return ResponseEntity.ok(CompanionSearchResponse.from(
                service.update(account.userId(), searchId, request.toCommand())));
    }

    @Override
    @PatchMapping(value = "/{searchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> changeStatus(@AuthenticationPrincipal AuthenticatedAccount account,
                                                                @PathVariable long searchId,
                                                                @Valid @RequestBody ChangeCompanionSearchStatusRequest request) {
        return ResponseEntity.ok(CompanionSearchResponse.from(
                service.changeStatus(account.userId(), searchId, request.status())));
    }
}
