package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
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
import java.time.ZoneOffset;

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
    private RoomMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMemberStatus status;

    // 안 읽은 메시지 수 계산용. 연관관계 없이 값으로만 둔다
    private Long lastReadMessageId;

    @Column(nullable = false)
    private boolean notificationMuted;

    // 마지막으로 들어온 시각. 처음 들어온 시각은 createdAt
    @Getter
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private RoomMember(Room room, Long userId, RoomMemberRole role) {
        this.room = room;
        this.userId = userId;
        this.role = role;
        this.status = RoomMemberStatus.JOINED;
        this.joinedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static RoomMember joinAsOwner(Room room, Long userId) {
        return new RoomMember(room, userId, RoomMemberRole.OWNER);
    }

    public static RoomMember joinAsMember(RoomInvitation invitation, Long userId, LocalDateTime now) {
        invitation.ensureValid(now);
        return new RoomMember(invitation.getRoom(), userId, RoomMemberRole.MEMBER);
    }

    public void rejoin(RoomInvitation invitation, LocalDateTime now) {
        if (status == RoomMemberStatus.JOINED) {
            return;
        }
        invitation.ensureValid(now);
        this.status = RoomMemberStatus.JOINED;
        this.joinedAt = now;
    }

    public void leave(LocalDateTime leftAt) {
        ensureJoined();
        this.status = RoomMemberStatus.LEFT;
        this.leftAt = leftAt;
    }

    public void ensureJoined() {
        if (status != RoomMemberStatus.JOINED) {
            throw new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    public RoomInvitation createShareInvitation(LocalDateTime issuedAt) {
        return RoomInvitation.byShare(room, userId, issuedAt);
    }

    public boolean isOwner() {
        return role.equals(RoomMemberRole.OWNER);
    }

}
