package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "companion_matching_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionMatchingJob extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;
    @Getter
    @Column(nullable = false)
    private long searchId;
    @Getter
    @Column(nullable = false)
    private long matchingRevision;
    @Getter
    @Column(nullable = false)
    private long candidateCursor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Getter
    private CompanionMatchingJobStatus status;
    @Column(nullable = false)
    private int attempts;
    private LocalDateTime availableAt;
    @Column(length = 36)
    private String claimToken;

    public CompanionMatchingJob(CompanionSearch search, LocalDateTime now) {
        searchId = search.getId();
        matchingRevision = search.getMatchingRevision();
        status = CompanionMatchingJobStatus.PENDING;
        availableAt = now;
    }

    public void claim(String token, LocalDateTime now) {
        status = CompanionMatchingJobStatus.PROCESSING;
        claimToken = token;
        attempts = Math.min(attempts + 1, 30);
        availableAt = now.plusMinutes(5);
    }

    public boolean isClaimedBy(String token) {
        return status == CompanionMatchingJobStatus.PROCESSING && token.equals(claimToken);
    }

    public void completeBatch(long cursor, boolean hasMore, LocalDateTime now) {
        candidateCursor = cursor;
        status = hasMore ? CompanionMatchingJobStatus.PENDING : CompanionMatchingJobStatus.SUCCEEDED;
        attempts = 0;
        availableAt = hasMore ? now : null;
        release();
    }

    public void skip() {
        status = CompanionMatchingJobStatus.SKIPPED;
        availableAt = null;
        release();
    }

    public String notificationEventKey(long targetSearchId) {
        return "companion-matching-job:" + id + ":search:" + targetSearchId;
    }

    public void retry(LocalDateTime now) {
        status = CompanionMatchingJobStatus.RETRY;
        availableAt = now.plusSeconds(Math.min(300L, 5L << Math.min(attempts - 1, 6)));
        release();
    }

    private void release() {
        claimToken = null;
    }
}
