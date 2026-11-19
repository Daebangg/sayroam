package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.CompanionDiscovery;
import com.daebangg.sayroam.companion.domain.CompanionDiscoveryRepository;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJob;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJobRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanionMatchingService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int BATCH_SIZE = 100;

    private final CompanionMatchingJobRepository jobs;
    private final CompanionSearchRepository searches;
    private final CompanionCandidateQueryRepository candidates;
    private final CompanionDiscoveryRepository discoveries;
    private final DestinationRepository destinations;

    @Transactional
    public Optional<CompanionMatchingClaim> claim() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return jobs.findNextForUpdate(now).map(job -> {
            String token = UUID.randomUUID().toString();
            job.claim(token, now);
            return new CompanionMatchingClaim(job.getId(), token);
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void process(CompanionMatchingClaim claim) {
        Optional<CompanionMatchingJob> found = jobs.findForUpdateById(claim.jobId());
        if (found.isEmpty() || !found.get().isClaimedBy(claim.token())) {
            return;
        }
        CompanionMatchingJob job = found.get();
        Optional<CompanionSearch> source = searches.findForUpdateById(job.getSearchId());
        LocalDate today = LocalDate.now(KOREA_ZONE);
        if (source.isEmpty() || !source.get().canMatch(job.getMatchingRevision(), today)
                || !destinations.existsByIdAndActiveTrue(source.get().getDestinationId())) {
            job.skip();
            return;
        }
        CompanionSearch search = source.get();
        // 원본 모집은 잠근 상태로 처리한다. 상대 모집은 조회 시점의 버전과 함께 발견 이력으로 남긴다.
        List<CompanionMatchingCandidateResult> matches = candidates.findMatchingCandidates(
                search, today, job.getCandidateCursor(), BATCH_SIZE);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (CompanionMatchingCandidateResult candidate : matches) {
            discoveries.record(CompanionDiscovery.found(search.getId(), search.getMatchingRevision(),
                    candidate.searchId(), candidate.matchingRevision(),
                    new TravelPeriod(candidate.overlapStart(), candidate.overlapEnd()), now));
        }
        long cursor = matches.isEmpty() ? job.getCandidateCursor() : matches.getLast().searchId();
        job.completeBatch(cursor, matches.size() == BATCH_SIZE, now);
    }

    @Transactional
    public void retry(CompanionMatchingClaim claim) {
        jobs.findForUpdateById(claim.jobId()).filter(job -> job.isClaimedBy(claim.token()))
                .ifPresent(job -> job.retry(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
