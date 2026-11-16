package com.daebangg.sayroam.companion.domain;

import java.util.List;
import java.util.Optional;

public interface CompanionSearchRepository {
    <S extends CompanionSearch> S save(S search);

    List<CompanionSearch> findByUserIdOrderByIdDesc(Long userId);

    Optional<CompanionSearch> findForUpdateByIdAndUserId(Long id, Long userId);
}
