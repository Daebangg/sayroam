package com.daebangg.sayroam.companion.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.companion.application.dto.PreviewCompanionCandidatesQuery;
import com.daebangg.sayroam.companion.application.service.CompanionSearchService;
import com.daebangg.sayroam.companion.presentation.api.CompanionCandidatePreviewApi;
import com.daebangg.sayroam.companion.presentation.dto.CompanionCandidateListResponse;
import com.daebangg.sayroam.user.domain.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class CompanionCandidatePreviewController implements CompanionCandidatePreviewApi {
    private final CompanionSearchService service;

    @Override
    @GetMapping(value = "/api/companion-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanionCandidateListResponse> preview(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam long destinationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Gender preferredGender,
            @RequestParam(required = false) Integer preferredMinAge,
            @RequestParam(required = false) Integer preferredMaxAge,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PreviewCompanionCandidatesQuery query = new PreviewCompanionCandidatesQuery(
                destinationId, startDate, endDate, preferredGender, preferredMinAge, preferredMaxAge, q);
        return ResponseEntity.ok(CompanionCandidateListResponse.from(
                service.previewCandidates(account.userId(), query, page, size)));
    }
}
