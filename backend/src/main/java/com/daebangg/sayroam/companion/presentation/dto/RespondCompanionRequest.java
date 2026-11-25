package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.domain.CompanionRequest;
import jakarta.validation.constraints.NotNull;

public record RespondCompanionRequest(@NotNull CompanionRequest.Decision status) {}
