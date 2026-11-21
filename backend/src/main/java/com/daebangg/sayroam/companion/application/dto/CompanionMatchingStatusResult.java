package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.CompanionMatchingStatus;

public record CompanionMatchingStatusResult(long searchId, long matchingRevision, CompanionMatchingStatus status) {
}
