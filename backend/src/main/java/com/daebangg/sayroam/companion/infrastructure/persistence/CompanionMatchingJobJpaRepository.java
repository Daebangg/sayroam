package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionMatchingJob;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJobRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface CompanionMatchingJobJpaRepository
        extends CompanionMatchingJobRepository, JpaRepository<CompanionMatchingJob, Long> {
    // 처리 중에는 available_at이 선점 만료 시각이며, 종료한 작업은 NULL로 큐 인덱스에서 제외한다.
    @Override
    @Query(value = """
            SELECT * FROM companion_matching_job
            WHERE available_at <= :now
            ORDER BY available_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<CompanionMatchingJob> findNextForUpdate(@Param("now") LocalDateTime now);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CompanionMatchingJob> findForUpdateById(Long id);

    @Override
    @Query("""
            select j from CompanionMatchingJob j
            where j.id in (select max(latest.id) from CompanionMatchingJob latest
                           where latest.searchId in :searchIds group by latest.searchId)
            """)
    List<CompanionMatchingJob> findLatestBySearchIds(@Param("searchIds") List<Long> searchIds);
}
