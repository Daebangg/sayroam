package com.daebangg.sayroam.companion.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface CompanionMatchingJobRepository {
    <S extends CompanionMatchingJob> S save(S job);
    Optional<CompanionMatchingJob> findNextForUpdate(LocalDateTime now);
    Optional<CompanionMatchingJob> findByIdForUpdate(Long jobId);

    List<CompanionMatchingJob> findLatestBySearchIds(List<Long> searchIds);
}
