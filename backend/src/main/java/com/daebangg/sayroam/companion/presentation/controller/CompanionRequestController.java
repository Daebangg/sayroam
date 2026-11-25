package com.daebangg.sayroam.companion.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.service.CompanionRequestService;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.presentation.api.CompanionRequestApi;
import com.daebangg.sayroam.companion.presentation.dto.CompanionRequestResponse;
import com.daebangg.sayroam.companion.presentation.dto.RespondCompanionRequest;
import com.daebangg.sayroam.companion.presentation.dto.SendCompanionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompanionRequestController implements CompanionRequestApi {
    private final CompanionRequestService service;

    @Override
    @PostMapping(value = "/companion-searches/{searchId}/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompanionRequestResponse send(@AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId,
                                         @Valid @RequestBody SendCompanionRequest body) {
        return CompanionRequestResponse.from(service.send(account.userId(), searchId, body.candidateSearchId()));
    }

    @Override
    @PatchMapping(value = "/companion-requests/{requestId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompanionRequestResponse respond(@AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long requestId,
                                            @Valid @RequestBody RespondCompanionRequest body) {
        return CompanionRequestResponse.from(service.respond(account.userId(), requestId, body.status()));
    }

    @Override
    @GetMapping("/companion-requests/{requestId}")
    public CompanionRequestResponse find(@AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long requestId) {
        return CompanionRequestResponse.from(service.find(account.userId(), requestId));
    }

    @Override
    @GetMapping("/companion-searches/{searchId}/requests")
    public List<CompanionRequestResponse> list(@AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId,
                                               @RequestParam(required = false) CompanionRequestStatus status) {
        return service.list(account.userId(), searchId, status).stream().map(CompanionRequestResponse::from).toList();
    }
}
