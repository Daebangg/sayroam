package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import com.daebangg.sayroam.companion.application.dto.PreviewCompanionCandidatesQuery;
import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.user.domain.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanionSearchService {
    public static final int MAX_CANDIDATE_PAGE_SIZE = 100;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final CompanionSearchRepository searches;
    private final DestinationRepository destinations;
    private final CompanionCandidateQueryRepository candidates;

    @Transactional
    public CompanionSearchResult create(long userId, SaveCompanionSearchCommand command) {
        requireActiveDestination(command.destinationId());
        CompanionSearch search = CompanionSearch.builder()
                .userId(userId)
                .destinationId(command.destinationId())
                .period(new TravelPeriod(command.startDate(), command.endDate()))
                .preferredGender(command.preferredGender())
                .preferredAgeRange(new AgeRange(command.preferredMinAge(), command.preferredMaxAge()))
                .title(command.title())
                .build();
        return CompanionSearchResult.from(searches.save(search));
    }

    @Transactional(readOnly = true)
    public List<CompanionSearchResult> findMine(long userId) {
        return searches.findByUserIdOrderByIdDesc(userId).stream().map(CompanionSearchResult::from).toList();
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page findCandidates(long userId, long searchId, String titleQuery,
                                                         int page, int size) {
        validateCandidatePage(page, size);
        CompanionSearch search = searches.findByIdAndUserId(searchId, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        search.validateCandidateSearch();
        return queryCandidates(userId, search.getDestinationId(), search.getPeriod(),
                search.getPreferredGender(), search.getPreferredAgeRange(), titleQuery, page, size);
    }

    @Transactional(readOnly = true)
    public CompanionCandidateResult.Page previewCandidates(long userId, PreviewCompanionCandidatesQuery query,
                                                           int page, int size) {
        validateCandidatePage(page, size);
        TravelPeriod period = new TravelPeriod(query.startDate(), query.endDate());
        AgeRange preferredAgeRange = new AgeRange(query.preferredMinAge(), query.preferredMaxAge());
        requireActiveDestination(query.destinationId());
        return queryCandidates(userId, query.destinationId(), period, query.preferredGender(), preferredAgeRange,
                query.titleQuery(), page, size);
    }

    private void validateCandidatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_CANDIDATE_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private CompanionCandidateResult.Page queryCandidates(long userId, long destinationId, TravelPeriod period,
                                                          Gender preferredGender, AgeRange preferredAgeRange,
                                                          String titleQuery, int page, int size) {
        String keyword = titleQuery == null ? "" : titleQuery.strip();
        if (keyword.codePointCount(0, keyword.length()) > CompanionSearch.MAX_TITLE_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        LocalDate ageReferenceDate = LocalDate.now(KOREA_ZONE);
        CompanionCandidateQueryRepository.CandidateQuery query = new CompanionCandidateQueryRepository.CandidateQuery(
                userId, destinationId, period, preferredGender, preferredAgeRange, keyword, ageReferenceDate,
                (long) page * size, size + 1);
        List<CompanionCandidateResult> results = candidates.findCandidates(query);
        boolean hasNext = results.size() > size;
        return new CompanionCandidateResult.Page(hasNext ? results.subList(0, size) : results, hasNext);
    }

    @Transactional
    public CompanionSearchResult update(long userId, long id, SaveCompanionSearchCommand command) {
        CompanionSearch search = findMineForUpdate(userId, id);
        requireActiveDestination(command.destinationId());
        search.update(command.destinationId(), new TravelPeriod(command.startDate(), command.endDate()),
                command.preferredGender(), new AgeRange(command.preferredMinAge(), command.preferredMaxAge()), command.title());
        return CompanionSearchResult.from(search);
    }

    @Transactional
    public CompanionSearchResult changeStatus(long userId, long id, CompanionSearchStatus status) {
        CompanionSearch search = findMineForUpdate(userId, id);
        search.changeStatus(status);
        return CompanionSearchResult.from(search);
    }

    private CompanionSearch findMineForUpdate(long userId, long id) {
        return searches.findForUpdateByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
    }

    private void requireActiveDestination(Long destinationId) {
        if (!destinations.existsByIdAndActiveTrue(destinationId)) {
            throw new BusinessException(CompanionSearchErrorCode.DESTINATION_UNAVAILABLE);
        }
    }
}
