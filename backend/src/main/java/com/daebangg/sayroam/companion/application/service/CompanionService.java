package com.daebangg.sayroam.companion.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.*;
import com.daebangg.sayroam.companion.application.port.out.CompanionQueryRepository;
import com.daebangg.sayroam.companion.domain.*;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.companion.domain.event.CompanionMatchingRequested;
import com.daebangg.sayroam.destination.application.service.DestinationService;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.notification.application.dto.CreateNotificationCommand;
import com.daebangg.sayroam.notification.application.service.NotificationService;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.user.application.service.UserService;
import com.daebangg.sayroam.user.domain.Gender;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CompanionService {
    public static final int MAX_CANDIDATE_PAGE_SIZE = 100;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final CompanionSearchRepository companionSearchRepository;
    private final CompanionRequestRepository companionRequestRepository;
    private final CompanionQueryRepository companionQueryRepository;
    private final CompanionMatchingJobRepository companionMatchingJobRepository;
    private final DestinationService destinationService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final RoomService roomService;
    private final ApplicationEventPublisher eventPublisher;

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
        companionSearchRepository.save(search);
        enqueueMatching(search);
        return result(search);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionSearchResult update(long userId, long id, SaveCompanionSearchCommand command) {
        lockOwner(userId);
        CompanionSearch search = findMineForUpdate(userId, id);
        requireActiveDestination(command.destinationId());
        if (command.travelStyle() != null) {
            search.updateTravelStyle(command.travelStyle());
        }
        search.requireCompleteTravelStyle();
        search.update(command.destinationId(), new TravelPeriod(command.startDate(), command.endDate()),
                command.preferredGender(), new AgeRange(command.preferredMinAge(), command.preferredMaxAge()),
                command.title(), command.introduction(), command.showAge(), command.showGender());
        companionSearchRepository.flush();
        refreshPending(id);
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
        companionSearchRepository.flush();
        if (status == CompanionSearchStatus.CLOSED) refreshPending(id);
        if (reopening) {
            enqueueMatching(search);
        }
        return result(search);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionRequestResult send(long userId, long searchId, long candidateSearchId) {
        long candidateOwner = companionSearchRepository.findOwnerId(candidateSearchId).orElseThrow(this::searchNotFound);
        lockMembers(userId, candidateOwner);
        CompanionSearch mine;
        CompanionSearch other;
        if (searchId <= candidateSearchId) {
            mine = findMineForUpdate(userId, searchId);
            other = lockSearch(candidateSearchId);
        } else {
            other = lockSearch(candidateSearchId);
            mine = findMineForUpdate(userId, searchId);
        }
        Optional<CompanionRequest> existing = companionRequestRepository.findBySearchPairForUpdate(
                Math.min(searchId, candidateSearchId), Math.max(searchId, candidateSearchId));
        if (existing.isPresent()) {
            CompanionRequest request = existing.get();
            expireIfInvalid(request);
            return result(request, userId);
        }
        if (!eligible(mine, other)) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_CANDIDATE_UNAVAILABLE);
        }
        CompanionRequest request = companionRequestRepository.save(mine.requestTo(userId, other, LocalDate.now(KOREA_ZONE)));
        notify(request.getReceiverId(), request, NotificationType.COMPANION_REQUEST, "You received a Roam Mate request.");
        return result(request, userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CompanionRequestResult respond(long userId, long requestId, CompanionRequest.Decision decision) {
        long otherId = companionRequestRepository.findOtherMemberId(userId, requestId).orElseThrow(this::requestNotFound);
        lockMembers(userId, otherId);
        List<Long> searchIds = companionRequestRepository.findSearchIdsForMember(userId, requestId);
        if (searchIds.size() != 2) throw requestNotFound();
        searchIds.forEach(this::lockSearch);
        // 단건 요청을 먼저 잠그지 않고, 두 모집의 대기 요청을 ID 순으로 함께 잠근다.
        List<CompanionRequest> pending = companionRequestRepository.findPendingForUpdate(searchIds);
        CompanionRequest request = ownedRequest(userId, requestId);
        boolean available = !request.isPending() || eligible(request, LocalDate.now(KOREA_ZONE));
        if (request.respond(userId, decision, available, LocalDateTime.now(ZoneOffset.UTC))) {
            boolean accepted = decision == CompanionRequest.Decision.ACCEPTED;
            if (accepted) {
                completeAcceptance(request, pending);
            }
            notify(request.getSenderId(), request,
                    accepted ? NotificationType.COMPANION_ACCEPTED : NotificationType.COMPANION_REJECTED,
                    accepted ? "Your Roam Mate request was accepted." : "Your Roam Mate request was declined.");
        }
        return result(request, userId);
    }

    private CompanionRequest ownedRequest(long userId, long requestId) {
        return companionRequestRepository.findAccessibleForUpdate(userId, requestId).orElseThrow(this::requestNotFound);
    }

    private void completeAcceptance(CompanionRequest request, List<CompanionRequest> pending) {
        Long roomId = roomService.createForCompanion();
        roomService.join(roomId, request.getSenderId());
        roomService.join(roomId, request.getReceiverId());
        request.linkRoom(roomId);
        // 수락된 요청의 expire()는 상태를 바꾸지 않는다.
        pending.forEach(CompanionRequest::expire);
    }

    public List<CompanionSearchResult> findMine(long userId) {
        List<CompanionSearch> mine = companionSearchRepository.findByUserIdOrderByIdDesc(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, CompanionMatchingJob> latest = companionMatchingJobRepository.findLatestBySearchIds(
                        mine.stream().map(CompanionSearch::getId).toList()).stream()
                .collect(Collectors.toMap(CompanionMatchingJob::getSearchId, job -> job));
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return mine.stream().map(search -> CompanionSearchResult.from(search,
                search.matchingStatus(latest.get(search.getId()), today))).toList();
    }

    public CompanionMatchingStatusResult findMatchingStatus(long userId, long searchId) {
        CompanionSearch search = companionSearchRepository.findAccessible(userId, searchId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        CompanionSearchResult result = result(search);
        return new CompanionMatchingStatusResult(searchId, result.matchingRevision(), result.matchingStatus());
    }

    public CompanionCandidateResult.Page findCandidates(long userId, long searchId, FindCompanionCandidatesQuery query) {
        validateCandidatePage(query.page(), query.size());
        CompanionSearch search = companionSearchRepository.findAccessible(userId, searchId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
        search.validateCandidateSearch();
        return queryCandidates(searchId, userId, search.getDestinationId(), search.getPeriod(),
                search.getPreferredGender(), search.getPreferredAgeRange(), query.titleQuery(), query.page(), query.size(), query.sort(),
                search.travelStyleAnswers());
    }

    public CompanionCandidateResult.Page previewCandidates(long userId, PreviewCompanionCandidatesQuery query) {
        validateCandidatePage(query.page(), query.size());
        TravelPeriod period = new TravelPeriod(query.startDate(), query.endDate());
        AgeRange preferredAgeRange = new AgeRange(query.preferredMinAge(), query.preferredMaxAge());
        requireActiveDestination(query.destinationId());
        return queryCandidates(null, userId, query.destinationId(), period, query.preferredGender(), preferredAgeRange,
                query.titleQuery(), query.page(), query.size(), query.sort(),
                query.travelStyle() == null ? findTravelStyleAnswers(userId)
                        : new RecruitmentTravelStyle(query.travelStyle()).answers());
    }

    public CompanionRequestResult find(long userId, long requestId) {
        CompanionRequest request = companionRequestRepository.findAccessible(userId, requestId).orElseThrow(this::requestNotFound);
        return result(request, userId);
    }

    public List<CompanionRequestResult> list(long userId, long searchId, @Nullable CompanionRequestStatus status) {
        companionSearchRepository.findAccessible(userId, searchId).orElseThrow(this::searchNotFound);
        List<CompanionRequest> found = companionRequestRepository.findForSearch(searchId);
        if (found.isEmpty()) return List.of();
        List<Long> memberIds = found.stream().map(request -> request.otherMemberId(userId)).distinct().toList();
        Map<Long, CompanionRequestResult.MemberProfile> profiles = companionQueryRepository.findMemberProfiles(memberIds)
                .stream().collect(Collectors.toMap(CompanionRequestResult.MemberProfile::userId, profile -> profile));
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return found.stream().map(request -> requestResult(request, userId,
                        profiles.get(request.otherMemberId(userId)), today))
                .filter(result -> status == null || result.status() == status).toList();
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
        CompanionQueryRepository.CandidateQuery query = new CompanionQueryRepository.CandidateQuery(
                searchId, userId, destinationId, period, preferredGender, preferredAgeRange, keyword, ageReferenceDate,
                (long) page * size, size + 1, sort == CompanionCandidateSort.STYLE, myAnswers);
        List<CompanionCandidateResult> results = companionQueryRepository.findCandidates(query);
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

    private void validateCandidatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_CANDIDATE_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private Map<String, Integer> findTravelStyleAnswers(long userId) {
        return companionQueryRepository.findTravelStyleAnswers(List.of(userId)).stream()
                .collect(Collectors.toMap(TravelStyleAnswerResult::code, TravelStyleAnswerResult::score));
    }

    private CompanionSearchResult result(CompanionSearch search) {
        CompanionMatchingJob latest = companionMatchingJobRepository.findLatestBySearchIds(List.of(search.getId())).stream()
                .findFirst().orElse(null);
        return CompanionSearchResult.from(search, search.matchingStatus(latest, LocalDate.now(KOREA_ZONE)));
    }

    private CompanionRequestResult result(CompanionRequest request, long userId) {
        CompanionRequestResult.MemberProfile profile = companionQueryRepository
                .findMemberProfiles(List.of(request.otherMemberId(userId))).stream().findFirst().orElse(null);
        return requestResult(request, userId, profile, LocalDate.now(KOREA_ZONE));
    }

    private CompanionRequestResult requestResult(CompanionRequest request, long userId,
                                                 CompanionRequestResult.@Nullable MemberProfile profile,
                                                 LocalDate today) {
        boolean available = !request.isPending() || eligible(request, today);
        return CompanionRequestResult.from(request, userId, profile, today, request.currentStatus(available));
    }

    private void enqueueMatching(CompanionSearch search) {
        CompanionMatchingJob job = companionMatchingJobRepository.save(new CompanionMatchingJob(search, LocalDateTime.now(ZoneOffset.UTC)));
        eventPublisher.publishEvent(new CompanionMatchingRequested(job.getId()));
    }

    private CompanionSearch findMineForUpdate(long userId, long searchId) {
        return companionSearchRepository.findAccessibleForUpdate(userId, searchId)
                .orElseThrow(() -> new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
    }

    private void lockOwner(long userId) {
        // 신규 등록과 상태 변경은 회원 → 모집 순으로 잠가 같은 회원의 활성 개수 변경을 직렬화한다.
        if (!userService.lockActiveUsers(List.of(userId)).contains(userId)) {
            throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
        }
    }

    private void lockMembers(long memberId, long otherId) {
        // 모집 종료와 같은 회원 → 모집/요청 순서를 지키고, 교차 요청은 작은 회원 ID부터 잠근다.
        userService.lockActiveUsers(List.of(memberId, otherId));
    }

    private CompanionSearch lockSearch(long searchId) {
        return companionSearchRepository.findByIdForUpdate(searchId).orElseThrow(this::searchNotFound);
    }

    private void requireAvailableActiveSlot(long userId) {
        // MySQL의 첫 일반 조회가 회원 잠금 획득 후 실행되도록 다른 조회보다 먼저 검사한다.
        CompanionSearch.validateActiveSearchCount(companionSearchRepository.countByUserIdAndStatus(userId, CompanionSearchStatus.ACTIVE));
    }

    private void requireActiveDestination(Long destinationId) {
        if (!destinationService.isAvailable(destinationId)) {
            throw new BusinessException(CompanionSearchErrorCode.DESTINATION_UNAVAILABLE);
        }
    }

    private void refreshPending(long searchId) {
        companionRequestRepository.findPendingForUpdate(searchId).forEach(this::expireIfInvalid);
    }

    private void expireIfInvalid(CompanionRequest request) {
        if (request.isPending()) {
            request.expireIfUnavailable(eligible(request, LocalDate.now(KOREA_ZONE)));
        }
    }

    private boolean eligible(CompanionSearch source, CompanionSearch other) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        return source.canMatch(today)
                && destinationService.isAvailable(source.getDestinationId())
                && companionQueryRepository.isMatchingCandidate(CompanionCandidateMatchQuery.from(source, today, other.getId()));
    }

    private boolean eligible(CompanionRequest request, LocalDate today) {
        return request.canMatch(today)
                && destinationService.isAvailable(request.matchingDestinationId())
                && companionQueryRepository.isMatchingCandidate(CompanionCandidateMatchQuery.from(request, today));
    }

    private void notify(long memberId, CompanionRequest request, NotificationType type, String content) {
        notificationService.createIfAbsent(new CreateNotificationCommand(memberId, type, NotificationTargetType.COMPANION_REQUEST,
                request.getId(), "companion-request:" + request.getId() + ":" + type.name(), content));
    }

    private BusinessException searchNotFound() {
        return new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND);
    }

    private BusinessException requestNotFound() {
        return new BusinessException(CompanionSearchErrorCode.COMPANION_REQUEST_NOT_FOUND);
    }
}
