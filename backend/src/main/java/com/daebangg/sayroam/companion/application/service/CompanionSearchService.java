package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final ApplicationEventPublisher events;

    @Transactional
    public CompanionSearchResult create(long userId, SaveCompanionSearchCommand command) {
        lockOwner(userId);
        requireAvailableActiveSlot(userId);
        requireCompleteTravelStyle(userId);
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
        searches.save(search);
        enqueueMatching(search);
        return CompanionSearchResult.from(search);
    }

    @Transactional(readOnly = true)
    public List<CompanionSearchResult> findMine(long userId) {
        return searches.findByUserIdOrderByIdDesc(userId).stream().map(CompanionSearchResult::from).toList();
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page findCandidates(long userId, long searchId, String titleQuery,
                                                         int page, int size, CompanionCandidateSort sort) {
        validateCandidatePage(page, size);
        CompanionSearch search = searches.findByIdAndUserId(searchId, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        search.validateCandidateSearch();
        return queryCandidates(userId, search.getDestinationId(), search.getPeriod(),
                search.getPreferredGender(), search.getPreferredAgeRange(), titleQuery, page, size, sort);
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page previewCandidates(long userId, PreviewCompanionCandidatesQuery query,
                                                           int page, int size, CompanionCandidateSort sort) {
        validateCandidatePage(page, size);
        TravelPeriod period = new TravelPeriod(query.startDate(), query.endDate());
        AgeRange preferredAgeRange = new AgeRange(query.preferredMinAge(), query.preferredMaxAge());
        requireActiveDestination(query.destinationId());
        return queryCandidates(userId, query.destinationId(), period, query.preferredGender(), preferredAgeRange,
                query.titleQuery(), page, size, sort);
    }

    private void validateCandidatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_CANDIDATE_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private CompanionCandidateResult.Page queryCandidates(long userId, long destinationId, TravelPeriod period,
                                                          Gender preferredGender, AgeRange preferredAgeRange,
                                                          String titleQuery, int page, int size, CompanionCandidateSort sort) {
        String keyword = titleQuery == null ? "" : titleQuery.strip();
        if (keyword.codePointCount(0, keyword.length()) > CompanionSearch.MAX_TITLE_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        Map<String, Integer> myAnswers = findTravelStyleAnswers(userId);
        if (sort == CompanionCandidateSort.STYLE && !TravelStyleMatch.isComplete(myAnswers)) {
            throw new BusinessException(CompanionSearchErrorCode.TRAVEL_STYLE_REQUIRED);
        }
        LocalDate ageReferenceDate = LocalDate.now(KOREA_ZONE);
        CompanionCandidateQueryRepository.CandidateQuery query = new CompanionCandidateQueryRepository.CandidateQuery(
                userId, destinationId, period, preferredGender, preferredAgeRange, keyword, ageReferenceDate,
                (long) page * size, size + 1, sort == CompanionCandidateSort.STYLE);
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
        List<Long> userIds = results.stream().map(CompanionCandidateResult::userId).toList();
        Map<Long, Map<String, Integer>> answersByUser = new HashMap<>();
        for (TravelStyleAnswerResult answer : candidates.findTravelStyleAnswers(userIds)) {
            answersByUser.computeIfAbsent(answer.userId(), ignored -> new HashMap<>()).put(answer.code(), answer.score());
        }
        List<CompanionCandidateResult> enriched = new ArrayList<>();
        for (CompanionCandidateResult candidate : results) {
            Map<String, Integer> answers = answersByUser.getOrDefault(candidate.userId(), Map.of());
            enriched.add(TravelStyleMatch.isComplete(answers)
                    ? candidate.withTravelStyleMatch(TravelStyleMatch.compare(myAnswers, answers)) : candidate);
        }
        return enriched;
    }

    private void requireCompleteTravelStyle(long userId) {
        if (!TravelStyleMatch.isComplete(findTravelStyleAnswers(userId))) {
            throw new BusinessException(CompanionSearchErrorCode.TRAVEL_STYLE_REQUIRED);
        }
    }

    private Map<String, Integer> findTravelStyleAnswers(long userId) {
        return candidates.findTravelStyleAnswers(List.of(userId)).stream()
                .collect(Collectors.toMap(TravelStyleAnswerResult::code, TravelStyleAnswerResult::score));
    }

    @Transactional
    public CompanionSearchResult update(long userId, long id, SaveCompanionSearchCommand command) {
        CompanionSearch search = findMineForUpdate(userId, id);
        requireActiveDestination(command.destinationId());
        search.update(command.destinationId(), new TravelPeriod(command.startDate(), command.endDate()),
                command.preferredGender(), new AgeRange(command.preferredMinAge(), command.preferredMaxAge()),
                command.title(), command.introduction(), command.showAge(), command.showGender());
        enqueueMatching(search);
        return CompanionSearchResult.from(search);
    }

    @Transactional
    public CompanionSearchResult changeStatus(long userId, long id, CompanionSearchStatus status) {
        lockOwner(userId);
        CompanionSearch search = findMineForUpdate(userId, id);
        boolean reopening = search.isReopening(status);
        if (reopening) {
            requireAvailableActiveSlot(userId);
            requireCompleteTravelStyle(userId);
            if (search.getDestinationId() != null) {
                requireActiveDestination(search.getDestinationId());
            }
        }
        search.changeStatus(status);
        if (reopening) {
            enqueueMatching(search);
        }
        return CompanionSearchResult.from(search);
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
