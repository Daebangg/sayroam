package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanionRequestMatchingTest {
    private final LocalDate endDate = LocalDate.of(2030, 4, 23);

    @Test
    void bothRecruitmentsCanMatchThroughTheOverlapEndDate() {
        CompanionRequest request = search(1L).requestTo(1L, search(2L), endDate);

        assertThat(request.canMatch(endDate)).isTrue();
        assertThat(request.canMatch(endDate.plusDays(1))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void closingEitherRecruitmentMakesTheRequestUnavailable(boolean closeSender) {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        CompanionRequest request = sender.requestTo(1L, receiver, endDate);

        (closeSender ? sender : receiver).changeStatus(CompanionSearchStatus.CLOSED);

        assertThat(request.canMatch(endDate)).isFalse();
        request.expireIfUnavailable(request.canMatch(endDate));
        assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.EXPIRED);
    }

    @Test
    void creatingARequestAlwaysChecksTheRecruitmentOwner() {
        assertThatThrownBy(() -> search(1L).requestTo(3L, search(2L), endDate))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CompanionSearchErrorCode.COMPANION_SEARCH_NOT_FOUND));
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed", "confirmed", "self", "destination", "period", "expired"})
    void creatingARequestRejectsUnavailableRecruitmentsWithoutAServiceCheck(String condition) {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        LocalDate today = endDate;
        switch (condition) {
            case "closed" -> receiver.changeStatus(CompanionSearchStatus.CLOSED);
            case "confirmed" -> receiver.confirm();
            case "self" -> receiver = search(1L);
            case "destination" -> receiver = CompanionSearch.builder().userId(2L).destinationId(2L)
                    .period(new TravelPeriod(endDate.minusDays(2), endDate)).build();
            case "period" -> receiver = CompanionSearch.builder().userId(2L).destinationId(1L)
                    .period(new TravelPeriod(endDate.plusDays(1), endDate.plusDays(3))).build();
            case "expired" -> today = endDate.plusDays(1);
            default -> throw new AssertionError(condition);
        }
        CompanionSearch candidate = receiver;
        LocalDate referenceDate = today;

        assertThatThrownBy(() -> sender.requestTo(1L, candidate, referenceDate))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CompanionSearchErrorCode.COMPANION_CANDIDATE_UNAVAILABLE));
    }

    @Test
    void acceptingARequestConfirmsBothRecruitmentsOnce() {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        CompanionRequest request = sender.requestTo(1L, receiver, endDate);
        LocalDateTime now = endDate.atStartOfDay();

        assertThat(request.respond(2L, CompanionRequest.Decision.ACCEPTED, true, now)).isTrue();
        assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.ACCEPTED);
        assertThat(sender.getStatus()).isEqualTo(CompanionSearchStatus.CONFIRMED);
        assertThat(receiver.getStatus()).isEqualTo(CompanionSearchStatus.CONFIRMED);
        long revision = sender.getMatchingRevision();
        assertThat(request.respond(2L, CompanionRequest.Decision.ACCEPTED, true, now.plusMinutes(1))).isFalse();
        assertThat(sender.getMatchingRevision()).isEqualTo(revision);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 3L})
    void respondingAlwaysChecksTheReceiverBeforeChangingState(long userId) {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        CompanionRequest request = sender.requestTo(1L, receiver, endDate);

        assertThatThrownBy(() -> request.respond(userId, CompanionRequest.Decision.ACCEPTED, false, endDate.atStartOfDay()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(userId == 1L ? CommonErrorCode.ACCESS_DENIED
                                : CompanionSearchErrorCode.COMPANION_REQUEST_NOT_FOUND));
        assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.PENDING);
        assertThat(sender.getStatus()).isEqualTo(CompanionSearchStatus.ACTIVE);
        assertThat(receiver.getStatus()).isEqualTo(CompanionSearchStatus.ACTIVE);
    }

    @Test
    void unavailableMatchingConditionsExpireTheRequestInsteadOfAccepting() {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        CompanionRequest request = sender.requestTo(1L, receiver, endDate);

        assertThat(request.respond(2L, CompanionRequest.Decision.ACCEPTED, false, endDate.atStartOfDay())).isFalse();
        assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.EXPIRED);
        assertThat(sender.getStatus()).isEqualTo(CompanionSearchStatus.ACTIVE);
        assertThat(receiver.getStatus()).isEqualTo(CompanionSearchStatus.ACTIVE);
    }

    @Test
    void invalidRecruitmentStateCannotPartiallyAcceptOrConfirmTheOtherRecruitment() {
        CompanionSearch sender = search(1L);
        CompanionSearch receiver = search(2L);
        CompanionRequest request = sender.requestTo(1L, receiver, endDate);
        receiver.changeStatus(CompanionSearchStatus.CLOSED);

        assertThatThrownBy(() -> request.respond(2L, CompanionRequest.Decision.ACCEPTED, true, endDate.atStartOfDay()))
                .isInstanceOf(BusinessException.class);
        assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.PENDING);
        assertThat(sender.getStatus()).isEqualTo(CompanionSearchStatus.ACTIVE);
        assertThat(receiver.getStatus()).isEqualTo(CompanionSearchStatus.CLOSED);
    }

    private CompanionSearch search(long userId) {
        return CompanionSearch.builder().userId(userId).destinationId(1L)
                .period(new TravelPeriod(endDate.minusDays(2), endDate)).build();
    }
}
