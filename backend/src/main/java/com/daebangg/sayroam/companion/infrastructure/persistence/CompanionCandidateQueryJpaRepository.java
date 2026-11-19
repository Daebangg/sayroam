package com.daebangg.sayroam.companion.infrastructure.persistence;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
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

    // 화면 후보 조회와 백그라운드 발견에서 같은 필수 조건을 사용한다.
    String ELIGIBLE_CANDIDATES = """
                where c.destinationId = :destinationId
                  and c.status = ACTIVE
                  and c.period.startDate <= :endDate
                  and c.period.endDate >= :startDate
                  and c.userId <> :ownerId
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
            """;

    @Override
    default List<CompanionCandidateResult> findCandidates(CandidateQuery query) {
        AgeRange ageRange = query.preferredAgeRange();
        return findCandidates(query.ownerId(), query.destinationId(), query.period().startDate(),
                query.period().endDate(), query.preferredGender(), ageRange == null ? null : ageRange.minAge(),
                ageRange == null ? null : ageRange.maxAge(), query.titleQuery(), query.ageReferenceDate(),
                query.offset(), query.limit(), query.styleSort(), query.travelStyle().get("planning"),
                query.travelStyle().get("daily-pace"), query.travelStyle().get("time-together"),
                query.travelStyle().get("spending"));
    }

    @Query("""
            with overlapping as (
                select c.id as searchId, c.userId as userId, candidate.nickname as nickname,
                       candidate.profileImageUrl as profileImageUrl, c.title as title,
                       c.period.startDate as startDate, c.period.endDate as endDate,
                       greatest(c.period.startDate, :startDate) as overlapStart,
                       least(c.period.endDate, :endDate) as overlapEnd,
                       abs(c.travelStyle.planning - :planning) + abs(c.travelStyle.dailyPace - :dailyPace)
                       + abs(c.travelStyle.timeTogether - :timeTogether) + abs(c.travelStyle.spending - :spending) as styleDistance
                from CompanionSearch c
                join User owner on owner.id = :ownerId and owner.deletedAt is null
                join User candidate on candidate.id = c.userId and candidate.deletedAt is null
            """ + ELIGIBLE_CANDIDATES + """
                  and (:titleQuery = '' or locate(:titleQuery, c.title) > 0)
                  and (:styleSort = false or (c.travelStyle.planning is not null
                       and c.travelStyle.dailyPace is not null and c.travelStyle.timeTogether is not null
                       and c.travelStyle.spending is not null))
            ),
            overlapsWithDays as (
                select o.searchId as searchId, o.userId as userId, o.nickname as nickname,
                       o.profileImageUrl as profileImageUrl, o.startDate as startDate, o.endDate as endDate,
                       o.overlapStart as overlapStart, o.overlapEnd as overlapEnd, o.styleDistance as styleDistance,
                       cast((o.overlapEnd - o.overlapStart) by day + 1 as Integer) as overlapDays, o.title as title
                from overlapping o
            ),
            ranked as (
                select o.searchId as searchId, o.nickname as nickname, o.profileImageUrl as profileImageUrl,
                       o.startDate as startDate, o.endDate as endDate, o.overlapStart as overlapStart,
                       o.overlapEnd as overlapEnd, o.overlapDays as overlapDays, o.title as title, o.styleDistance as styleDistance,
                       row_number() over (
                           partition by o.userId order by case when :styleSort = true then o.styleDistance else 0 end,
                                                        o.overlapDays desc, o.overlapStart, o.searchId
                       ) as memberRank
                from overlapsWithDays o
            ),
            orderedCandidates as (
                select r.searchId as searchId, search.userId as userId, r.nickname as nickname, r.profileImageUrl as profileImageUrl,
                       r.startDate as startDate, r.endDate as endDate, r.overlapStart as overlapStart,
                       r.overlapEnd as overlapEnd, r.overlapDays as overlapDays, r.title as title,
                       row_number() over (
                           order by case when :styleSort = true then r.styleDistance else 0 end,
                                    r.overlapDays desc, r.overlapStart, r.searchId
                       ) as candidateRank
                from ranked r
                join CompanionSearch search on search.id = r.searchId
                where r.memberRank = 1
            )
            select new com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult(
                r.searchId, r.nickname, r.profileImageUrl, r.startDate, r.endDate,
                r.overlapStart, r.overlapEnd, r.overlapDays, r.title, search.introduction,
                case when search.showAge then cast((:ageReferenceDate - profile.birthDate) by year as Integer)
                     else null end,
                case when search.showGender then profile.gender else null end, r.userId, search.travelStyle
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
            @Param("styleSort") boolean styleSort,
            @Param("planning") Integer planning,
            @Param("dailyPace") Integer dailyPace,
            @Param("timeTogether") Integer timeTogether,
            @Param("spending") Integer spending);

    @Override
    default List<CompanionMatchingCandidateResult> findMatchingCandidates(CompanionSearch source, LocalDate today,
                                                                          long afterId, int limit) {
        AgeRange ages = source.getPreferredAgeRange();
        return findMatchingCandidates(source.getUserId(), source.getDestinationId(), source.getPeriod().startDate(),
                source.getPeriod().endDate(), source.getPreferredGender(), ages == null ? null : ages.minAge(),
                ages == null ? null : ages.maxAge(), today, afterId, limit);
    }

    @Query("""
            select new com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult(
                c.id, c.matchingRevision, greatest(c.period.startDate, :startDate), least(c.period.endDate, :endDate))
            from CompanionSearch c
            join User owner on owner.id = :ownerId and owner.deletedAt is null
            join User candidate on candidate.id = c.userId and candidate.deletedAt is null
            """ + ELIGIBLE_CANDIDATES + """
              and c.id > :afterId and least(c.period.endDate, :endDate) >= :ageReferenceDate
            order by c.id
            limit :limit
            """)
    List<CompanionMatchingCandidateResult> findMatchingCandidates(
            @Param("ownerId") long ownerId,
            @Param("destinationId") long destinationId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("preferredGender") Gender preferredGender,
            @Param("preferredMinAge") Integer preferredMinAge,
            @Param("preferredMaxAge") Integer preferredMaxAge,
            @Param("ageReferenceDate") LocalDate today,
            @Param("afterId") long afterId,
            @Param("limit") int limit);

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
