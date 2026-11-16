package com.daebangg.sayroam.companion.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = "searches")
public record CompanionSearchListResponse(List<CompanionSearchResponse> searches) {
}
