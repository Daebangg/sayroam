package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = "searches")
public record CompanionSearchListResponse(List<CompanionSearchResponse> searches) {
    public static CompanionSearchListResponse from(List<CompanionSearchResult> results) {
        return new CompanionSearchListResponse(results.stream()
                .map(CompanionSearchResponse::from)
                .toList());
    }
}
