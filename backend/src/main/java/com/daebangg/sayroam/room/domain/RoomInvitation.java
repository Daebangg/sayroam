package com.daebangg.sayroam.room.domain;

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

// 서비스 안에서 회원을 지정해 보내는 초대(invitee, status)와
// 카카오톡 공유 메시지 버튼에 넣는 초대(code, 여러 명 사용 가능)를 함께 담는다.
// 수락하면 RoomMember가 생기고, 초대받기만 한 사람은 방 멤버가 아니다
@Entity
@Table(
        name = "room_invitation",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_invitation_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    // 서비스 안 초대만 있다. 공유 초대는 NULL
    @Column(name = "invitee_id")
    private Long inviteeId;

    // 공유 초대만 있다. 서비스 안 초대는 NULL
    @Column(length = 50)
    private String code;

    // 서비스 안 초대만 있다. 공유 초대는 NULL
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomInvitationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private RoomInvitation(Room room, Long inviterId, Long inviteeId, String code, RoomInvitationStatus status,
                           LocalDateTime expiresAt) {
        this.room = room;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.code = code;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public static RoomInvitation toMember(Room room, Long inviterId, Long inviteeId, LocalDateTime expiresAt) {
        return new RoomInvitation(room, inviterId, inviteeId, null, RoomInvitationStatus.PENDING, expiresAt);
    }

    public static RoomInvitation byShare(Room room, Long inviterId, String code, LocalDateTime expiresAt) {
        return new RoomInvitation(room, inviterId, null, code, null, expiresAt);
    }

}
