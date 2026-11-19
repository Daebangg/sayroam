package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.domain.CompanionDiscovery;
import com.daebangg.sayroam.companion.domain.CompanionDiscoveryRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface CompanionDiscoveryJpaRepository extends CompanionDiscoveryRepository, Repository<CompanionDiscovery, Long> {
    @Override
    default void record(CompanionDiscovery discovery) {
        upsert(discovery);
    }

    // 반대 방향 작업이 늦게 저장해도 최신 조건 버전의 발견 정보를 되돌리지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO companion_discovery
                (first_search_id, second_search_id, first_revision, second_revision,
                 overlap_start, overlap_end, last_matched_at, created_at, updated_at)
            VALUES (:#{#d.firstSearchId}, :#{#d.secondSearchId}, :#{#d.firstRevision}, :#{#d.secondRevision},
                    :#{#d.overlapStart}, :#{#d.overlapEnd}, :#{#d.lastMatchedAt},
                    :#{#d.lastMatchedAt}, :#{#d.lastMatchedAt})
            ON DUPLICATE KEY UPDATE
                overlap_start = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                   :#{#d.overlapStart}, overlap_start),
                overlap_end = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                 :#{#d.overlapEnd}, overlap_end),
                last_matched_at = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                     GREATEST(last_matched_at, :#{#d.lastMatchedAt}), last_matched_at),
                updated_at = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                GREATEST(updated_at, :#{#d.lastMatchedAt}), updated_at),
                first_revision = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                    :#{#d.firstRevision}, first_revision),
                second_revision = IF(first_revision <= :#{#d.firstRevision} AND second_revision <= :#{#d.secondRevision},
                                     :#{#d.secondRevision}, second_revision)
            """, nativeQuery = true)
    void upsert(@Param("d") CompanionDiscovery discovery);
}
