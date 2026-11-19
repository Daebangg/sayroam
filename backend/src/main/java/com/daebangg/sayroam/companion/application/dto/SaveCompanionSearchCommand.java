package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

public record SaveCompanionSearchCommand(
        Long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        Gender preferredGender,
        Integer preferredMinAge,
        Integer preferredMaxAge,
        String title,
        @Nullable String introduction,
        @Nullable Boolean showAge,
        @Nullable Boolean showGender
) {
}
