package com.daebangg.sayroam.companion.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.service.CompanionService;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.presentation.api.CompanionApi;
import com.daebangg.sayroam.companion.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CompanionController implements CompanionApi {
    private final CompanionService companionService;

    @Override
    @PostMapping(value = "/companion-searches", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> create(@AuthenticationPrincipal AuthenticatedAccount account,
                                                          @Valid @RequestBody SaveCompanionSearchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CompanionSearchResponse.from(companionService.create(account.userId(), request.toCommand())));
    }

    @Override
    @PutMapping(value = "/companion-searches/{searchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> update(@AuthenticationPrincipal AuthenticatedAccount account,
                                                          @PathVariable long searchId,
                                                          @Valid @RequestBody SaveCompanionSearchRequest request) {
        return ResponseEntity.ok(CompanionSearchResponse.from(
                companionService.update(account.userId(), searchId, request.toCommand())));
    }

    @Override
    @PatchMapping(value = "/companion-searches/{searchId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionSearchResponse> changeStatus(@AuthenticationPrincipal AuthenticatedAccount account,
                                                                @PathVariable long searchId,
                                                                @Valid @RequestBody ChangeCompanionSearchStatusRequest request) {
        return ResponseEntity.ok(CompanionSearchResponse.from(
                companionService.changeStatus(account.userId(), searchId, request.status())));
    }

    @Override
    @PostMapping(value = "/companion-searches/{searchId}/requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionRequestResponse> send(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId,
            @Valid @RequestBody SendCompanionRequest body) {
        return ResponseEntity.ok(CompanionRequestResponse.from(
                companionService.send(account.userId(), searchId, body.candidateSearchId())));
    }

    @Override
    @PatchMapping(value = "/companion-requests/{requestId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionRequestResponse> respond(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long requestId,
            @Valid @RequestBody RespondCompanionRequest body) {
        return ResponseEntity.ok(CompanionRequestResponse.from(
                companionService.respond(account.userId(), requestId, body.status())));
    }

    @Override
    @GetMapping("/companion-searches/me")
    public ResponseEntity<CompanionSearchListResponse> findMine(@AuthenticationPrincipal AuthenticatedAccount account) {
        return ResponseEntity.ok(CompanionSearchListResponse.from(companionService.findMine(account.userId())));
    }

    @Override
    @GetMapping("/companion-searches/{searchId}/matching-status")
    public ResponseEntity<CompanionMatchingStatusResponse> findMatchingStatus(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId) {
        return ResponseEntity.ok(CompanionMatchingStatusResponse.from(
                companionService.findMatchingStatus(account.userId(), searchId)));
    }

    @Override
    @GetMapping("/companion-searches/{searchId}/candidates")
    public ResponseEntity<CompanionCandidateListResponse> findCandidates(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId,
            @Valid @ModelAttribute FindCompanionCandidatesRequest request) {
        return ResponseEntity.ok(CompanionCandidateListResponse.from(
                companionService.findCandidates(account.userId(), searchId, request.toQuery())));
    }

    @Override
    @GetMapping(value = "/companion-candidates")
    public ResponseEntity<CompanionCandidateListResponse> preview(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @ModelAttribute PreviewCompanionCandidatesRequest request) {
        return ResponseEntity.ok(CompanionCandidateListResponse.from(
                companionService.previewCandidates(account.userId(), request.toQuery())));
    }

    @Override
    @GetMapping("/companion-requests/{requestId}")
    public ResponseEntity<CompanionRequestResponse> find(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long requestId) {
        return ResponseEntity.ok(CompanionRequestResponse.from(companionService.find(account.userId(), requestId)));
    }

    @Override
    @GetMapping("/companion-searches/{searchId}/requests")
    public ResponseEntity<List<CompanionRequestResponse>> list(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable long searchId,
            @RequestParam(required = false) CompanionRequestStatus status) {
        List<CompanionRequestResponse> responses = companionService.list(account.userId(), searchId, status).stream()
                .map(CompanionRequestResponse::from).toList();
        return ResponseEntity.ok(responses);
    }
}
