package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingStatusResult;
import com.daebangg.sayroam.companion.application.dto.PreviewCompanionCandidatesQuery;
import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.application.dto.TravelStyleAnswerResult;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionCandidateSort;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJob;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJobRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.companion.domain.RecruitmentTravelStyle;
import com.daebangg.sayroam.companion.domain.TravelStyleMatch;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.companion.domain.event.CompanionMatchingRequested;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.user.domain.Gender;
import com.daebangg.sayroam.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanionSearchService {
    public static final int MAX_CANDIDATE_PAGE_SIZE = 100;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final CompanionSearchRepository searches;
    private final DestinationRepository destinations;
    private final CompanionCandidateQueryRepository candidates;
    private final UserRepository users;
    private final CompanionMatchingJobRepository matchingJobs;
    private final CompanionRequestService requests;
    private final ApplicationEventPublisher events;

    @Transactional
    public CompanionSearchResult create(long userId, SaveCompanionSearchCommand command) {
        lockOwner(userId);
        requireAvailableActiveSlot(userId);
        requireActiveDestination(command.destinationId());
        CompanionSearch search = CompanionSearch.builder()
                .userId(userId)
                .destinationId(command.destinationId())
                .period(new TravelPeriod(command.startDate(), command.endDate()))
                .preferredGender(command.preferredGender())
                .preferredAgeRange(new AgeRange(command.preferredMinAge(), command.preferredMaxAge()))
                .title(command.title())
                .introduction(command.introduction())
                .showAge(command.showAge())
                .showGender(command.showGender())
                .build();
        search.updateTravelStyle(command.travelStyle() == null
                ? findTravelStyleAnswers(userId) : command.travelStyle());
        searches.save(search);
        enqueueMatching(search);
        return result(search);
    }

    @Transactional(readOnly = true)
    public List<CompanionSearchResult> findMine(long userId) {
        List<CompanionSearch> mine = searches.findByUserIdOrderByIdDesc(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, CompanionMatchingJob> latest = matchingJobs.findLatestBySearchIds(
                        mine.stream().map(CompanionSearch::getId).toList()).stream()
                .collect(Collectors.toMap(CompanionMatchingJob::getSearchId, job -> job));
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return mine.stream().map(search -> CompanionSearchResult.from(search,
                search.matchingStatus(latest.get(search.getId()), today))).toList();
    }

    @Transactional(readOnly = true)
    public CompanionMatchingStatusResult findMatchingStatus(long userId, long searchId) {
        CompanionSearch search = searches.findByIdAndUserId(searchId, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        CompanionSearchResult result = result(search);
        return new CompanionMatchingStatusResult(searchId, result.matchingRevision(), result.matchingStatus());
    }

    private CompanionSearchResult result(CompanionSearch search) {
        CompanionMatchingJob latest = matchingJobs.findLatestBySearchIds(List.of(search.getId())).stream()
                .findFirst().orElse(null);
        return CompanionSearchResult.from(search, search.matchingStatus(latest, LocalDate.now(KOREA_ZONE)));
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page findCandidates(long userId, long searchId, String titleQuery,
                                                         int page, int size, CompanionCandidateSort sort) {
        validateCandidatePage(page, size);
        CompanionSearch search = searches.findByIdAndUserId(searchId, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        search.validateCandidateSearch();
        return queryCandidates(searchId, userId, search.getDestinationId(), search.getPeriod(),
                search.getPreferredGender(), search.getPreferredAgeRange(), titleQuery, page, size, sort,
                search.travelStyleAnswers());
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page previewCandidates(long userId, PreviewCompanionCandidatesQuery query,
                                                           int page, int size, CompanionCandidateSort sort) {
        validateCandidatePage(page, size);
        TravelPeriod period = new TravelPeriod(query.startDate(), query.endDate());
        AgeRange preferredAgeRange = new AgeRange(query.preferredMinAge(), query.preferredMaxAge());
        requireActiveDestination(query.destinationId());
        return queryCandidates(null, userId, query.destinationId(), period, query.preferredGender(), preferredAgeRange,
                query.titleQuery(), page, size, sort,
                query.travelStyle() == null ? findTravelStyleAnswers(userId)
                        : new RecruitmentTravelStyle(query.travelStyle()).answers());
    }

    private void validateCandidatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_CANDIDATE_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private CompanionCandidateResult.Page queryCandidates(Long searchId, long userId, long destinationId, TravelPeriod period,
                                                          Gender preferredGender, AgeRange preferredAgeRange,
                                                          String titleQuery, int page, int size, CompanionCandidateSort sort,
                                                          Map<String, Integer> myAnswers) {
        String keyword = titleQuery == null ? "" : titleQuery.strip();
        if (keyword.codePointCount(0, keyword.length()) > CompanionSearch.MAX_TITLE_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (sort == CompanionCandidateSort.STYLE && !TravelStyleMatch.isComplete(myAnswers)) {
            throw new BusinessException(CompanionSearchErrorCode.TRAVEL_STYLE_REQUIRED);
        }
        LocalDate ageReferenceDate = LocalDate.now(KOREA_ZONE);
        CompanionCandidateQueryRepository.CandidateQuery query = new CompanionCandidateQueryRepository.CandidateQuery(
                searchId, userId, destinationId, period, preferredGender, preferredAgeRange, keyword, ageReferenceDate,
                (long) page * size, size + 1, sort == CompanionCandidateSort.STYLE, myAnswers);
        List<CompanionCandidateResult> results = candidates.findCandidates(query);
        boolean hasNext = results.size() > size;
        List<CompanionCandidateResult> pageResults = hasNext ? results.subList(0, size) : results;
        return new CompanionCandidateResult.Page(addTravelStyleMatches(pageResults, myAnswers), hasNext);
    }

    private List<CompanionCandidateResult> addTravelStyleMatches(List<CompanionCandidateResult> results,
                                                                 Map<String, Integer> myAnswers) {
        if (results.isEmpty() || !TravelStyleMatch.isComplete(myAnswers)) {
            return results;
        }
        List<CompanionCandidateResult> enriched = new ArrayList<>();
        for (CompanionCandidateResult candidate : results) {
            Map<String, Integer> answers = candidate.travelStyle() == null ? Map.of() : candidate.travelStyle().answers();
            enriched.add(TravelStyleMatch.isComplete(answers)
                    ? candidate.withTravelStyleMatch(TravelStyleMatch.compare(myAnswers, answers)) : candidate);
        }
        return enriched;
    }

    private Map<String, Integer> findTravelStyleAnswers(long userId) {
        return candidates.findTravelStyleAnswers(List.of(userId)).stream()
                .collect(Collectors.toMap(TravelStyleAnswerResult::code, TravelStyleAnswerResult::score));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionSearchResult update(long userId, long id, SaveCompanionSearchCommand command) {
        CompanionSearch search = findMineForUpdate(userId, id);
        requireActiveDestination(command.destinationId());
        if (command.travelStyle() != null) {
            search.updateTravelStyle(command.travelStyle());
        }
        search.requireCompleteTravelStyle();
        search.update(command.destinationId(), new TravelPeriod(command.startDate(), command.endDate()),
                command.preferredGender(), new AgeRange(command.preferredMinAge(), command.preferredMaxAge()),
                command.title(), command.introduction(), command.showAge(), command.showGender());
        requests.refreshPending(id);
        enqueueMatching(search);
        return result(search);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionSearchResult changeStatus(long userId, long id, CompanionSearchStatus status) {
        lockOwner(userId);
        CompanionSearch search = findMineForUpdate(userId, id);
        boolean reopening = search.isReopening(status);
        if (reopening) {
            requireAvailableActiveSlot(userId);
            if (search.getDestinationId() != null) {
                requireActiveDestination(search.getDestinationId());
            }
        }
        search.changeStatus(status);
        if (status == CompanionSearchStatus.CLOSED) requests.refreshPending(id);
        if (reopening) {
            enqueueMatching(search);
        }
        return result(search);
    }

    private void enqueueMatching(CompanionSearch search) {
        CompanionMatchingJob job = matchingJobs.save(new CompanionMatchingJob(search, LocalDateTime.now(ZoneOffset.UTC)));
        events.publishEvent(new CompanionMatchingRequested(job.getId()));
    }

    private CompanionSearch findMineForUpdate(long userId, long id) {
        return searches.findForUpdateByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
    }

    private void lockOwner(long userId) {
        // 신규 등록과 상태 변경은 회원 → 모집 순으로 잠가 같은 회원의 활성 개수 변경을 직렬화한다.
        users.findForUpdateByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));
    }

    private void requireAvailableActiveSlot(long userId) {
        // MySQL의 첫 일반 조회가 회원 잠금 획득 후 실행되도록 다른 조회보다 먼저 검사한다.
        CompanionSearch.validateActiveSearchCount(searches.countByUserIdAndStatus(userId, CompanionSearchStatus.ACTIVE));
    }

    private void requireActiveDestination(Long destinationId) {
        if (!destinations.existsByIdAndActiveTrue(destinationId)) {
            throw new BusinessException(CompanionSearchErrorCode.DESTINATION_UNAVAILABLE);
        }
    }
}
