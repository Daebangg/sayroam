package com.daebangg.sayroam.companion.domain;

import java.util.List;
import java.util.Optional;

public interface CompanionSearchRepository {
    <S extends CompanionSearch> S save(S search);

    long countByUserIdAndStatus(Long userId, CompanionSearchStatus status);

    List<CompanionSearch> findByUserIdOrderByIdDesc(Long userId);

    Optional<CompanionSearch> findByIdAndUserId(Long id, Long userId);

    Optional<CompanionSearch> findForUpdateById(Long id);

    Optional<CompanionSearch> findForUpdateByIdAndUserId(Long id, Long userId);
}
