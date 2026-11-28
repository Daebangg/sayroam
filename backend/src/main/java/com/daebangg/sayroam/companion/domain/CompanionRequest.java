package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "companion_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionRequest extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false)
    @Getter
    private Long senderId;

    @Column(nullable = false)
    @Getter
    private Long receiverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_search_id")
    @Getter
    private CompanionSearch senderSearch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_search_id", nullable = false)
    @Getter
    private CompanionSearch receiverSearch;

    // DB가 발신·수신 모집으로부터 계산한다. 과거 단방향 요청은 두 값이 모두 NULL이다.
    @Column(insertable = false, updatable = false)
    private Long firstSearchId;

    @Column(insertable = false, updatable = false)
    private Long secondSearchId;

    @Getter
    @Column(name = "room_id", unique = true)
    private Long roomId;

    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Getter
    private CompanionRequestStatus status;

    private LocalDateTime respondedAt;

    CompanionRequest(CompanionSearch sender, CompanionSearch receiver) {
        if (sender.getUserId().equals(receiver.getUserId())) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_CANDIDATE_UNAVAILABLE);
        }
        this.senderSearch = sender;
        this.receiverSearch = receiver;
        this.senderId = sender.getUserId();
        this.receiverId = receiver.getUserId();
        this.status = CompanionRequestStatus.PENDING;
    }

    public boolean respond(long userId, Decision decision, boolean matchingAvailable, LocalDateTime now) {
        requireReceiver(userId);
        if (decision == null || now == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        expireIfUnavailable(matchingAvailable);
        if (status == decision.status || status == CompanionRequestStatus.EXPIRED) return false;
        if (status != CompanionRequestStatus.PENDING) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_REQUEST_ALREADY_ANSWERED);
        }
        if (decision == Decision.ACCEPTED) {
            senderSearch.validateCandidateSearch();
            receiverSearch.validateCandidateSearch();
        }
        status = decision.status;
        respondedAt = now;
        if (decision == Decision.ACCEPTED) {
            confirmSearches();
        }
        return true;
    }

    private void requireReceiver(long userId) {
        if (direction(userId) != Direction.RECEIVED) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }
    }

    private void confirmSearches() {
        ensureAccepted();
        senderSearch.confirm();
        receiverSearch.confirm();
    }

    public void linkRoom(Long roomId) {
        ensureAccepted();
        if (roomId == null || roomId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (this.roomId != null) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_REQUEST_ALREADY_ANSWERED);
        }
        this.roomId = roomId;
    }

    public void expire() {
        expireIfUnavailable(false);
    }

    public void expireIfUnavailable(boolean available) {
        status = currentStatus(available);
    }

    public Direction direction(long userId) {
        if (senderId == userId) return Direction.SENT;
        if (receiverId == userId) return Direction.RECEIVED;
        throw new BusinessException(CompanionSearchErrorCode.COMPANION_REQUEST_NOT_FOUND);
    }

    public long otherMemberId(long userId) {
        return direction(userId) == Direction.SENT ? receiverId : senderId;
    }

    public boolean canMatch(LocalDate today) {
        return senderSearch.canMatch(today) && receiverSearch.canMatch(today);
    }

    public Long matchingDestinationId() {
        return senderSearch.getDestinationId();
    }

    public boolean isPending() {
        return status == CompanionRequestStatus.PENDING;
    }

    public CompanionRequestStatus currentStatus(boolean available) {
        return isPending() && !available ? CompanionRequestStatus.EXPIRED : status;
    }

    private void ensureAccepted() {
        if (status != CompanionRequestStatus.ACCEPTED) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }

    public enum Direction { SENT, RECEIVED }

    public enum Decision {
        ACCEPTED(CompanionRequestStatus.ACCEPTED), REJECTED(CompanionRequestStatus.REJECTED);

        private final CompanionRequestStatus status;

        Decision(CompanionRequestStatus status) {
            this.status = status;
        }
    }
}
