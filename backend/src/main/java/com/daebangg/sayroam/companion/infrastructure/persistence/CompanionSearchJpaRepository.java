package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanionSearchJpaRepository extends CompanionSearchRepository, JpaRepository<CompanionSearch, Long> {
    @Override
    @Query("select s.userId from CompanionSearch s where s.id = :searchId")
    Optional<Long> findOwnerId(@Param("searchId") long searchId);

    @Override
    @Query("select s from CompanionSearch s where s.userId = :userId and s.id = :searchId")
    Optional<CompanionSearch> findAccessible(@Param("userId") Long userId, @Param("searchId") Long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CompanionSearch s where s.id = :searchId")
    Optional<CompanionSearch> findByIdForUpdate(@Param("searchId") Long searchId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CompanionSearch s where s.userId = :userId and s.id = :searchId")
    Optional<CompanionSearch> findAccessibleForUpdate(@Param("userId") Long userId, @Param("searchId") Long searchId);
}
