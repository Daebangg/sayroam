package com.daebangg.sayroam.companion.domain;

public enum CompanionMatchingStatus {
    NOT_REQUESTED, PENDING, PROCESSING, RETRY, SUCCEEDED, SKIPPED;

    public static CompanionMatchingStatus from(CompanionMatchingJobStatus status) {
        return switch (status) {
            case PENDING -> PENDING;
            case PROCESSING -> PROCESSING;
            case RETRY -> RETRY;
            case SUCCEEDED -> SUCCEEDED;
            case SKIPPED -> SKIPPED;
        };
    }
}
