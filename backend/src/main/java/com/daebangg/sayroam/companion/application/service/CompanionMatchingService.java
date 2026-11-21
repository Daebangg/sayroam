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
import com.daebangg.sayroam.notification.application.dto.CreateNotificationCommand;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.user.domain.UserRepository;
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
    private final UserRepository users;
    private final NotificationService notifications;

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
        // 배치 조회 후 상대 모집·프로필을 다시 확인하고 발견과 양쪽 알림을 함께 커밋한다.
        List<CompanionMatchingCandidateResult> matches = candidates.findMatchingCandidates(
                search, today, job.getCandidateCursor(), BATCH_SIZE);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (CompanionMatchingCandidateResult candidate : matches) {
            recordDiscovery(job, search, candidate, today, now);
        }
        long cursor = matches.isEmpty() ? job.getCandidateCursor() : matches.getLast().searchId();
        job.completeBatch(cursor, matches.size() == BATCH_SIZE, now);
    }

    private void recordDiscovery(CompanionMatchingJob job, CompanionSearch source, CompanionMatchingCandidateResult candidate,
                                 LocalDate today, LocalDateTime now) {
        Optional<CompanionSearch> found = searches.findForUpdateById(candidate.searchId());
        if (found.isEmpty() || !found.get().canMatch(candidate.matchingRevision(), today)) {
            return;
        }
        CompanionSearch target = found.get();
        // 프로필 변경·탈퇴와 알림 생성을 직렬화한다. 양방향 작업의 교착은 기존 작업 재시도로 복구한다.
        long firstUserId = Math.min(source.getUserId(), target.getUserId());
        long secondUserId = Math.max(source.getUserId(), target.getUserId());
        if (users.findForUpdateByIdAndDeletedAtIsNull(firstUserId).isEmpty()
                || users.findForUpdateByIdAndDeletedAtIsNull(secondUserId).isEmpty()
                || !destinations.existsByIdAndActiveTrue(source.getDestinationId())
                || !candidates.isMatchingCandidate(source, today, target.getId())) {
            return;
        }
        CompanionDiscovery discovery = CompanionDiscovery.found(source.getId(), source.getMatchingRevision(),
                target.getId(), target.getMatchingRevision(),
                new TravelPeriod(candidate.overlapStart(), candidate.overlapEnd()), now);
        // 양쪽 모집을 잠근 상태이므로 반대 방향 작업과 신규 발견 판별도 직렬화된다.
        boolean newlyDiscovered = discoveries.findByFirstSearchIdAndSecondSearchId(
                discovery.getFirstSearchId(), discovery.getSecondSearchId()).isEmpty();
        discoveries.record(discovery);
        if (newlyDiscovered) {
            notifyDiscovery(source, job.notificationEventKey(source.getId()));
            notifyDiscovery(target, job.notificationEventKey(target.getId()));
        }
    }

    private void notifyDiscovery(CompanionSearch search, String eventKey) {
        String title = search.getTitle() == null ? "your recruitment" : "\"" + search.getTitle() + "\"";
        notifications.createIfAbsent(new CreateNotificationCommand(search.getUserId(),
                NotificationType.COMPANION_MATCH_FOUND, NotificationTargetType.COMPANION_SEARCH,
                search.getId(), eventKey, "New travel companion candidates were found for " + title + "."));
    }

    @Transactional
    public void retry(CompanionMatchingClaim claim) {
        jobs.findForUpdateById(claim.jobId()).filter(job -> job.isClaimedBy(claim.token()))
                .ifPresent(job -> job.retry(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
