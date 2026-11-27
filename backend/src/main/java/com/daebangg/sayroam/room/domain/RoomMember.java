package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 나간 멤버도 정산 대상이라 행을 지우지 않고 status로 남긴다
@Entity
@Table(
        name = "room_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_member_room_user", columnNames = {"room_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMemberStatus status;

    @Getter
    private Long lastReadMessageId;

    @Column(nullable = false)
    private boolean notificationMuted;

    // 마지막으로 들어온 시각. 처음 들어온 시각은 createdAt
    @Getter
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // 지금 나가 있는 경우에만 나간 시각을 가진다. 다시 참여하면 비운다
    private LocalDateTime leftAt;

    private RoomMember(Room room, Long userId, LocalDateTime joinedAt) {
        this.room = room;
        this.userId = userId;
        this.status = RoomMemberStatus.JOINED;
        this.joinedAt = joinedAt;
    }

    public static RoomMember join(Room room, Long userId, LocalDateTime now) {
        return new RoomMember(room, userId, now);
    }

    public boolean rejoin(LocalDateTime now) {
        if (status == RoomMemberStatus.JOINED) {
            return false;
        }
        this.status = RoomMemberStatus.JOINED;
        this.joinedAt = now;
        this.leftAt = null;
        return true;
    }

    public void leave(LocalDateTime leftAt) {
        ensureJoined();
        this.status = RoomMemberStatus.LEFT;
        this.leftAt = leftAt;
    }

    public void advanceLastRead(Long messageId) {
        ensureJoined();
        if (lastReadMessageId != null && lastReadMessageId >= messageId) {
            return;
        }
        this.lastReadMessageId = messageId;
    }

    public void ensureJoined() {
        if (status != RoomMemberStatus.JOINED) {
            throw new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED);
        }
    }
}
