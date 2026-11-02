package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "companion_request",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_companion_request_sender_receiver_search",
                columnNames = {"sender_id", "receiver_id", "companion_search_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_search_id", nullable = false)
    private CompanionSearch companionSearch;

    // 수락 시 생성되는 1:1 방. 수락 전에는 NULL
    @Column(name = "room_id", unique = true)
    private Long roomId;

    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionRequestStatus status;

    private LocalDateTime respondedAt;

    // 받은 회원이 동행 찾기 등록자와 어긋나지 않도록 CompanionSearch.requestBy로만 만든다
    CompanionRequest(Long senderId, Long receiverId, CompanionSearch companionSearch, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.companionSearch = companionSearch;
        this.message = message;
        this.status = CompanionRequestStatus.PENDING;
    }

}
