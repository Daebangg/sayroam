package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Map;

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
        @Nullable Boolean showGender,
        @Nullable Map<String, Integer> travelStyle
) {
    public SaveCompanionSearchCommand(Long destinationId, LocalDate startDate, LocalDate endDate,
                                      Gender preferredGender, Integer preferredMinAge, Integer preferredMaxAge,
                                      String title, String introduction, Boolean showAge, Boolean showGender) {
        this(destinationId, startDate, endDate, preferredGender, preferredMinAge, preferredMaxAge,
                title, introduction, showAge, showGender, null);
    }
}
