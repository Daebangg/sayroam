package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.companion.application.dto.CompanionRequestResult;
import com.daebangg.sayroam.companion.application.port.out.CompanionCandidateQueryRepository;
import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestRepository;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchRepository;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.notification.application.dto.CreateNotificationCommand;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanionRequestService {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final CompanionRequestRepository requests;
    private final CompanionSearchRepository searches;
    private final CompanionCandidateQueryRepository candidates;
    private final DestinationRepository destinations;
    private final UserRepository users;
    private final NotificationService notifications;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionRequestResult send(long userId, long searchId, long candidateSearchId) {
        long candidateOwner = searches.findOwnerId(candidateSearchId).orElseThrow(this::searchNotFound);
        lockMembers(userId, candidateOwner);
        CompanionSearch first = lockSearch(Math.min(searchId, candidateSearchId));
        CompanionSearch second = lockSearch(Math.max(searchId, candidateSearchId));
        CompanionSearch mine = first.getId() == searchId ? first : second;
        CompanionSearch other = first.getId() == searchId ? second : first;
        if (mine.getUserId() != userId) throw searchNotFound();
        Optional<CompanionRequest> existing = requests.findByFirstSearchIdAndSecondSearchId(first.getId(), second.getId());
        if (existing.isPresent()) {
            CompanionRequest request = existing.get();
            expireIfInvalid(request);
            return result(request, userId);
        }
        if (!eligible(mine, other)) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_CANDIDATE_UNAVAILABLE);
        }
        CompanionRequest request = requests.save(new CompanionRequest(mine, other));
        notify(request.getReceiverId(), request, NotificationType.COMPANION_REQUEST, "You received a Roam Mate request.");
        return result(request, userId);
    }

    // 모집 종료와 같은 회원 → 모집/요청 순서를 지키고, 교차 요청은 작은 회원 ID부터 잠근다.
    private void lockMembers(long memberId, long otherId) {
        users.findForUpdateByIdAndDeletedAtIsNull(Math.min(memberId, otherId));
        users.findForUpdateByIdAndDeletedAtIsNull(Math.max(memberId, otherId));
    }

    private CompanionSearch lockSearch(long id) {
        return searches.findForUpdateById(id).orElseThrow(this::searchNotFound);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionRequestResult respond(long userId, long requestId, CompanionRequest.Decision decision) {
        long otherId = requests.findOtherMemberId(requestId, userId).orElseThrow(this::requestNotFound);
        lockMembers(userId, otherId);
        CompanionRequest request = ownedRequest(userId, requestId);
        if (request.direction(userId) != CompanionRequest.Direction.RECEIVED) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }
        expireIfInvalid(request);
        if (request.respond(decision, LocalDateTime.now(ZoneOffset.UTC))) {
            boolean accepted = decision == CompanionRequest.Decision.ACCEPTED;
            notify(request.getSenderId(), request,
                    accepted ? NotificationType.COMPANION_ACCEPTED : NotificationType.COMPANION_REJECTED,
                    accepted ? "Your Roam Mate request was accepted." : "Your Roam Mate request was declined.");
        }
        return result(request, userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionRequestResult find(long userId, long requestId) {
        CompanionRequest request = ownedRequest(userId, requestId);
        expireIfInvalid(request);
        return result(request, userId);
    }

    private CompanionRequest ownedRequest(long userId, long requestId) {
        return requests.findOwnedForUpdate(requestId, userId).orElseThrow(this::requestNotFound);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<CompanionRequestResult> list(long userId, long searchId, @Nullable CompanionRequestStatus status) {
        searches.findByIdAndUserId(searchId, userId).orElseThrow(this::searchNotFound);
        if (status == null || status == CompanionRequestStatus.PENDING || status == CompanionRequestStatus.EXPIRED) {
            refreshPending(searchId);
        }
        List<CompanionRequest> found = requests.findForSearch(searchId, status);
        if (found.isEmpty()) return List.of();
        List<Long> memberIds = found.stream().map(request -> request.otherMemberId(userId)).distinct().toList();
        Map<Long, User> profiles = users.findByIdInAndDeletedAtIsNull(memberIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        LocalDate today = LocalDate.now(KOREA);
        return found.stream().map(request -> CompanionRequestResult.from(request, userId,
                profiles.get(request.otherMemberId(userId)), today)).toList();
    }

    void refreshPending(long searchId) {
        requests.findPendingForUpdate(searchId).forEach(this::expireIfInvalid);
    }

    private void expireIfInvalid(CompanionRequest request) {
        if (request.getStatus() == CompanionRequestStatus.PENDING
                && !eligible(request.getSenderSearch(), request.getReceiverSearch())) {
            request.expire();
        }
    }

    private boolean eligible(CompanionSearch source, CompanionSearch other) {
        LocalDate today = LocalDate.now(KOREA);
        return source.canMatch(source.getMatchingRevision(), today)
                && destinations.existsByIdAndActiveTrue(source.getDestinationId())
                && candidates.isMatchingCandidate(source, today, other.getId());
    }

    private CompanionRequestResult result(CompanionRequest request, long userId) {
        long otherId = request.otherMemberId(userId);
        User profile = users.findByIdAndDeletedAtIsNull(otherId).orElse(null);
        return CompanionRequestResult.from(request, userId, profile, LocalDate.now(KOREA));
    }

    private void notify(long memberId, CompanionRequest request, NotificationType type, String content) {
        notifications.createIfAbsent(new CreateNotificationCommand(memberId, type, NotificationTargetType.COMPANION_REQUEST,
                request.getId(), "companion-request:" + request.getId() + ":" + type.name(), content));
    }

    private BusinessException searchNotFound() {
        return new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND);
    }

    private BusinessException requestNotFound() {
        return new BusinessException(CompanionSearchErrorCode.COMPANION_REQUEST_NOT_FOUND);
    }
}
