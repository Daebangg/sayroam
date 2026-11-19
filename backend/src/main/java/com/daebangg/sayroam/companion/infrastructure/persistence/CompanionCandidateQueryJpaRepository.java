package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.TravelStyleAnswerResult;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.user.domain.Gender;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface CompanionCandidateQueryJpaRepository
        extends CompanionCandidateQueryRepository, Repository<CompanionSearch, Long> {

    @Override
    default List<CompanionCandidateResult> findCandidates(CandidateQuery query) {
        AgeRange ageRange = query.preferredAgeRange();
        return findCandidates(query.ownerId(), query.destinationId(), query.period().startDate(),
                query.period().endDate(), query.preferredGender(), ageRange == null ? null : ageRange.minAge(),
                ageRange == null ? null : ageRange.maxAge(), query.titleQuery(), query.ageReferenceDate(),
                query.offset(), query.limit(), query.styleSort());
    }

    @Query("""
            with overlapping as (
                select c.id as searchId, c.userId as userId, candidate.nickname as nickname,
                       candidate.profileImageUrl as profileImageUrl, c.title as title,
                       c.period.startDate as startDate, c.period.endDate as endDate,
                       greatest(c.period.startDate, :startDate) as overlapStart,
                       least(c.period.endDate, :endDate) as overlapEnd
                from CompanionSearch c
                join User owner on owner.id = :ownerId and owner.deletedAt is null
                join User candidate on candidate.id = c.userId and candidate.deletedAt is null
                where c.destinationId = :destinationId
                  and c.status = ACTIVE
                  and c.period.startDate <= :endDate
                  and c.period.endDate >= :startDate
                  and c.userId <> :ownerId
                  and (:titleQuery = '' or locate(:titleQuery, c.title) > 0)
                  and (:preferredGender is null or candidate.gender = :preferredGender)
                  and (c.preferredGender is null or owner.gender = c.preferredGender)
                  and (:preferredMinAge is null
                       or (:ageReferenceDate - candidate.birthDate) by year >= :preferredMinAge)
                  and (:preferredMaxAge is null
                       or (:ageReferenceDate - candidate.birthDate) by year <= :preferredMaxAge)
                  and (c.preferredAgeRange.minAge is null
                       or (:ageReferenceDate - owner.birthDate) by year >= c.preferredAgeRange.minAge)
                  and (c.preferredAgeRange.maxAge is null
                       or (:ageReferenceDate - owner.birthDate) by year <= c.preferredAgeRange.maxAge)
            ),
            overlapsWithDays as (
                select o.searchId as searchId, o.userId as userId, o.nickname as nickname,
                       o.profileImageUrl as profileImageUrl, o.startDate as startDate, o.endDate as endDate,
                       o.overlapStart as overlapStart, o.overlapEnd as overlapEnd,
                       cast((o.overlapEnd - o.overlapStart) by day + 1 as Integer) as overlapDays, o.title as title
                from overlapping o
            ),
            ranked as (
                select o.searchId as searchId, o.nickname as nickname, o.profileImageUrl as profileImageUrl,
                       o.startDate as startDate, o.endDate as endDate, o.overlapStart as overlapStart,
                       o.overlapEnd as overlapEnd, o.overlapDays as overlapDays, o.title as title,
                       row_number() over (
                           partition by o.userId order by o.overlapDays desc, o.overlapStart, o.searchId
                       ) as memberRank
                from overlapsWithDays o
            ),
            styleDistances as (
                select answer.userId as userId, sum(abs(answer.score - mine.score)) as distance
                from UserTravelStyle answer
                join UserTravelStyle mine on mine.userId = :ownerId and mine.item.id = answer.item.id
                join TravelStyleItem question on question.id = answer.item.id
                where :styleSort = true and question.active = true
                  and question.code in ('planning', 'daily-pace', 'time-together', 'spending')
                  and answer.score between 1 and 5 and mine.score between 1 and 5
                  and answer.userId in (select candidate.userId from overlapping candidate)
                group by answer.userId
                having count(*) = 4
            ),
            orderedCandidates as (
                select r.searchId as searchId, search.userId as userId, r.nickname as nickname, r.profileImageUrl as profileImageUrl,
                       r.startDate as startDate, r.endDate as endDate, r.overlapStart as overlapStart,
                       r.overlapEnd as overlapEnd, r.overlapDays as overlapDays, r.title as title,
                       row_number() over (
                           order by case when :styleSort = true then style.distance else 0 end,
                                    r.overlapDays desc, r.overlapStart, r.searchId
                       ) as candidateRank
                from ranked r
                join CompanionSearch search on search.id = r.searchId
                left join styleDistances style on style.userId = search.userId
                where r.memberRank = 1 and (:styleSort = false or style.distance is not null)
            )
            select new com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult(
                r.searchId, r.nickname, r.profileImageUrl, r.startDate, r.endDate,
                r.overlapStart, r.overlapEnd, r.overlapDays, r.title, search.introduction,
                case when search.showAge then cast((:ageReferenceDate - profile.birthDate) by year as Integer)
                     else null end,
                case when search.showGender then profile.gender else null end, r.userId
            )
            from orderedCandidates r
            join CompanionSearch search on search.id = r.searchId
            join User profile on profile.id = search.userId
            where r.candidateRank > :offset
            order by r.candidateRank
            limit :limit
            """)
    List<CompanionCandidateResult> findCandidates(
            @Param("ownerId") long ownerId,
            @Param("destinationId") long destinationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("preferredGender") Gender preferredGender,
            @Param("preferredMinAge") Integer preferredMinAge,
            @Param("preferredMaxAge") Integer preferredMaxAge,
            @Param("titleQuery") String titleQuery,
            @Param("ageReferenceDate") LocalDate ageReferenceDate,
            @Param("offset") long offset,
            @Param("limit") int limit,
            @Param("styleSort") boolean styleSort);

    @Override
    @Query("""
            select new com.daebangg.sayroam.companion.application.dto.TravelStyleAnswerResult(
                answer.userId, question.code, answer.score)
            from UserTravelStyle answer
            join answer.item question
            where answer.userId in :userIds and question.active = true
              and question.code in ('planning', 'daily-pace', 'time-together', 'spending')
            """)
    List<TravelStyleAnswerResult> findTravelStyleAnswers(@Param("userIds") List<Long> userIds);
}
