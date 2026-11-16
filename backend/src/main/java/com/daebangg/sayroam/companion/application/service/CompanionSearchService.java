package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionSearchResult;
import com.daebangg.sayroam.companion.application.dto.SaveCompanionSearchCommand;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanionSearchService {
    private final CompanionSearchRepository searches;
    private final DestinationRepository destinations;

    @Transactional
    public CompanionSearchResult create(long userId, SaveCompanionSearchCommand command) {
        requireActiveDestination(command.destinationId());
        CompanionSearch search = CompanionSearch.builder()
                .userId(userId)
                .destinationId(command.destinationId())
                .period(new TravelPeriod(command.startDate(), command.endDate()))
                .preferredGender(command.preferredGender())
                .preferredAgeRange(new AgeRange(command.preferredMinAge(), command.preferredMaxAge()))
                .build();
        return CompanionSearchResult.from(searches.save(search));
    }

    @Transactional(readOnly = true)
    public List<CompanionSearchResult> findMine(long userId) {
        return searches.findByUserIdOrderByIdDesc(userId).stream().map(CompanionSearchResult::from).toList();
    }

    @Transactional
    public CompanionSearchResult update(long userId, long id, SaveCompanionSearchCommand command) {
        CompanionSearch search = findMineForUpdate(userId, id);
        requireActiveDestination(command.destinationId());
        search.update(command.destinationId(), new TravelPeriod(command.startDate(), command.endDate()),
                command.preferredGender(), new AgeRange(command.preferredMinAge(), command.preferredMaxAge()));
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
