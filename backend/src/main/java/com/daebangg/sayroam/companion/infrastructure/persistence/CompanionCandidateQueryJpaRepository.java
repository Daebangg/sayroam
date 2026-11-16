package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface CompanionCandidateQueryJpaRepository
        extends CompanionCandidateQueryRepository, Repository<CompanionSearch, Long> {

    @Override
    default List<CompanionCandidateResult> findCandidates(CandidateQuery query) {
        return findCandidates(query.ownerId(), query.searchId(), query.ageReferenceDate(), query.offset(), query.limit());
    }

    @Query("""
            with overlapping as (
                select c.id as searchId, c.userId as userId, candidate.nickname as nickname,
                       candidate.profileImageUrl as profileImageUrl,
                       c.period.startDate as startDate, c.period.endDate as endDate,
                       greatest(c.period.startDate, s.period.startDate) as overlapStart,
                       least(c.period.endDate, s.period.endDate) as overlapEnd
                from CompanionSearch s
                join User owner on owner.id = s.userId and owner.deletedAt is null
                join CompanionSearch c on c.destinationId = s.destinationId
                    and c.status = ACTIVE
                    and c.period.startDate <= s.period.endDate
                    and c.period.endDate >= s.period.startDate
                    and c.userId <> s.userId
                join User candidate on candidate.id = c.userId and candidate.deletedAt is null
                where s.id = :searchId and s.userId = :ownerId
                  and (s.preferredGender is null or candidate.gender = s.preferredGender)
                  and (c.preferredGender is null or owner.gender = c.preferredGender)
                  and (s.preferredAgeRange.minAge is null
                       or (:ageReferenceDate - candidate.birthDate) by year >= s.preferredAgeRange.minAge)
                  and (s.preferredAgeRange.maxAge is null
                       or (:ageReferenceDate - candidate.birthDate) by year <= s.preferredAgeRange.maxAge)
                  and (c.preferredAgeRange.minAge is null
                       or (:ageReferenceDate - owner.birthDate) by year >= c.preferredAgeRange.minAge)
                  and (c.preferredAgeRange.maxAge is null
                       or (:ageReferenceDate - owner.birthDate) by year <= c.preferredAgeRange.maxAge)
            ),
            overlapsWithDays as (
                select o.searchId as searchId, o.userId as userId, o.nickname as nickname,
                       o.profileImageUrl as profileImageUrl, o.startDate as startDate, o.endDate as endDate,
                       o.overlapStart as overlapStart, o.overlapEnd as overlapEnd,
                       cast((o.overlapEnd - o.overlapStart) by day + 1 as Integer) as overlapDays
                from overlapping o
            ),
            ranked as (
                select o.searchId as searchId, o.nickname as nickname, o.profileImageUrl as profileImageUrl,
                       o.startDate as startDate, o.endDate as endDate, o.overlapStart as overlapStart,
                       o.overlapEnd as overlapEnd, o.overlapDays as overlapDays,
                       row_number() over (
                           partition by o.userId order by o.overlapDays desc, o.overlapStart, o.searchId
                       ) as memberRank
                from overlapsWithDays o
            ),
            orderedCandidates as (
                select r.searchId as searchId, r.nickname as nickname, r.profileImageUrl as profileImageUrl,
                       r.startDate as startDate, r.endDate as endDate, r.overlapStart as overlapStart,
                       r.overlapEnd as overlapEnd, r.overlapDays as overlapDays,
                       row_number() over (
                           order by r.overlapDays desc, r.overlapStart, r.searchId
                       ) as candidateRank
                from ranked r
                where r.memberRank = 1
            )
            select new com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult(
                r.searchId, r.nickname, r.profileImageUrl, r.startDate, r.endDate,
                r.overlapStart, r.overlapEnd, r.overlapDays
            )
            from orderedCandidates r
            where r.candidateRank > :offset
            order by r.candidateRank
            limit :limit
            """)
    List<CompanionCandidateResult> findCandidates(
            @Param("ownerId") long ownerId,
            @Param("searchId") long searchId,
            @Param("ageReferenceDate") LocalDate ageReferenceDate,
            @Param("offset") long offset,
            @Param("limit") int limit);
}
