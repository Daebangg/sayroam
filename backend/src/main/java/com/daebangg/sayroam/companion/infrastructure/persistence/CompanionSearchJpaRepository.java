package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CompanionSearchJpaRepository extends CompanionSearchRepository, JpaRepository<CompanionSearch, Long> {
    @Override
    @Query("select s.userId from CompanionSearch s where s.id = :searchId")
    Optional<Long> findOwnerId(long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompanionSearch> findForUpdateById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompanionSearch> findForUpdateByIdAndUserId(Long id, Long userId);
}
