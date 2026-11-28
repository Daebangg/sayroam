package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateMatchQuery;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingSourceResult;
import com.daebangg.sayroam.companion.application.port.out.CompanionQueryRepository;
import com.daebangg.sayroam.companion.domain.CompanionDiscovery;
import com.daebangg.sayroam.companion.domain.CompanionDiscoveryRepository;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJob;
import com.daebangg.sayroam.companion.domain.CompanionMatchingJobRepository;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.destination.application.service.DestinationService;
import com.daebangg.sayroam.notification.application.dto.CreateNotificationCommand;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanionMatchingService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int BATCH_SIZE = 100;

    private final CompanionMatchingJobRepository companionMatchingJobRepository;
    private final CompanionSearchRepository companionSearchRepository;
    private final CompanionQueryRepository companionQueryRepository;
    private final CompanionDiscoveryRepository companionDiscoveryRepository;
    private final DestinationService destinationService;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public Optional<CompanionMatchingClaim> claim() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return companionMatchingJobRepository.findNextForUpdate(now).map(job -> {
            String token = UUID.randomUUID().toString();
            job.claim(token, now);
            return new CompanionMatchingClaim(job.getId(), token);
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void process(CompanionMatchingClaim claim) {
        Optional<CompanionMatchingJob> found = companionMatchingJobRepository.findByIdForUpdate(claim.jobId());
        if (found.isEmpty() || !found.get().isClaimedBy(claim.token())) {
            return;
        }
        CompanionMatchingJob job = found.get();
        Optional<CompanionMatchingSourceResult> snapshot = companionQueryRepository.findMatchingSource(job.getSearchId());
        if (snapshot.isEmpty()) {
            job.skip();
            return;
        }
        LocalDate today = LocalDate.now(KOREA_ZONE);
        List<CompanionMatchingCandidateResult> matches = companionQueryRepository.findMatchingCandidates(
                snapshot.get(), today, job.getCandidateCursor(), BATCH_SIZE);
        List<Long> userIds = new ArrayList<>();
        userIds.add(snapshot.get().userId());
        matches.forEach(candidate -> userIds.add(candidate.userId()));
        // 배치 전체를 회원 ID → 모집 ID 순으로 잠가 요청 수락·모집 변경과 같은 순서를 지킨다.
        Set<Long> activeUserIds = userService.lockActiveUsers(userIds);
        Set<Long> searchIds = new TreeSet<>();
        searchIds.add(job.getSearchId());
        matches.forEach(candidate -> searchIds.add(candidate.searchId()));
        Map<Long, CompanionSearch> lockedSearches = new LinkedHashMap<>();
        for (Long searchId : searchIds) {
            companionSearchRepository.findByIdForUpdate(searchId).ifPresent(search -> lockedSearches.put(searchId, search));
        }
        CompanionSearch search = lockedSearches.get(job.getSearchId());
        if (search == null || !activeUserIds.contains(search.getUserId())
                || !search.canMatch(job.getMatchingRevision(), today)
                || !destinationService.isAvailable(search.getDestinationId())) {
            job.skip();
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Map<Long, CompanionSearch> recipients = new LinkedHashMap<>();
        for (CompanionMatchingCandidateResult candidate : matches) {
            Optional<CompanionSearch> discovered = recordDiscovery(search, candidate, lockedSearches.get(candidate.searchId()),
                    activeUserIds, today, now);
            if (discovered.isPresent()) {
                CompanionSearch target = discovered.get();
                recipients.put(search.getId(), search);
                recipients.put(target.getId(), target);
            }
        }
        for (CompanionSearch recipient : recipients.values()) {
            notifyDiscovery(recipient, job.notificationEventKey(recipient.getId()));
        }
        long cursor = matches.isEmpty() ? job.getCandidateCursor() : matches.getLast().searchId();
        job.completeBatch(cursor, matches.size() == BATCH_SIZE, now);
    }

    private Optional<CompanionSearch> recordDiscovery(CompanionSearch source, CompanionMatchingCandidateResult candidate,
                                                     CompanionSearch target, Set<Long> activeUserIds,
                                                     LocalDate today, LocalDateTime now) {
        if (target == null || !activeUserIds.contains(target.getUserId())
                || !target.canMatch(candidate.matchingRevision(), today)
                || !destinationService.isAvailable(source.getDestinationId())
                || !companionQueryRepository.isMatchingCandidate(CompanionCandidateMatchQuery.from(source, today, target.getId()))) {
            return Optional.empty();
        }
        CompanionDiscovery discovery = CompanionDiscovery.found(source.getId(), source.getMatchingRevision(),
                target.getId(), target.getMatchingRevision(),
                new TravelPeriod(candidate.overlapStart(), candidate.overlapEnd()), now);
        // 양쪽 모집을 잠근 상태이므로 반대 방향 작업과 신규 발견 판별도 직렬화된다.
        boolean newlyDiscovered = companionDiscoveryRepository.findByFirstSearchIdAndSecondSearchId(
                discovery.getFirstSearchId(), discovery.getSecondSearchId()).isEmpty();
        companionDiscoveryRepository.record(discovery);
        return newlyDiscovered ? Optional.of(target) : Optional.empty();
    }

    private void notifyDiscovery(CompanionSearch search, String eventKey) {
        String title = search.getTitle() == null ? "your recruitment" : "\"" + search.getTitle() + "\"";
        notificationService.createIfAbsent(new CreateNotificationCommand(search.getUserId(),
                NotificationType.COMPANION_MATCH_FOUND, NotificationTargetType.COMPANION_SEARCH,
                search.getId(), eventKey, "New travel companion candidates were found for " + title + "."));
    }

    @Transactional
    public void retry(CompanionMatchingClaim claim) {
        companionMatchingJobRepository.findByIdForUpdate(claim.jobId()).filter(job -> job.isClaimedBy(claim.token()))
                .ifPresent(job -> job.retry(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
