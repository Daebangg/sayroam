package com.daebangg.sayroam.companion.application.dto;

import java.time.LocalDate;

public record CompanionMatchingCandidateResult(long searchId, long userId, long matchingRevision,
                                                LocalDate overlapStart, LocalDate overlapEnd) {
}
