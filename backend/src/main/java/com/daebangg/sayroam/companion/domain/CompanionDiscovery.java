package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "companion_discovery", uniqueConstraints =
        @UniqueConstraint(name = "uk_companion_discovery_pair", columnNames = {"first_search_id", "second_search_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionDiscovery extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    @Column(nullable = false)
    private long firstSearchId;
    @Getter
    @Column(nullable = false)
    private long secondSearchId;
    @Getter
    @Column(nullable = false)
    private long firstRevision;
    @Getter
    @Column(nullable = false)
    private long secondRevision;
    @Getter
    @Column(nullable = false)
    private LocalDate overlapStart;
    @Getter
    @Column(nullable = false)
    private LocalDate overlapEnd;
    @Getter
    @Column(nullable = false)
    private LocalDateTime lastMatchedAt;

    public static CompanionDiscovery found(long sourceId, long sourceRevision, long candidateId,
                                            long candidateRevision, TravelPeriod overlap, LocalDateTime now) {
        CompanionDiscovery discovery = new CompanionDiscovery();
        boolean sourceFirst = sourceId < candidateId;
        discovery.firstSearchId = sourceFirst ? sourceId : candidateId;
        discovery.secondSearchId = sourceFirst ? candidateId : sourceId;
        discovery.firstRevision = sourceFirst ? sourceRevision : candidateRevision;
        discovery.secondRevision = sourceFirst ? candidateRevision : sourceRevision;
        discovery.overlapStart = overlap.startDate();
        discovery.overlapEnd = overlap.endDate();
        discovery.lastMatchedAt = now;
        return discovery;
    }
}
