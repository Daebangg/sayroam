package com.daebangg.sayroam.companion.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SendCompanionRequest(@NotNull @Positive Long candidateSearchId) {}
