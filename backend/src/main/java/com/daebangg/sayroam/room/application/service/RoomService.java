package com.daebangg.sayroam.room.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;
import com.daebangg.sayroam.room.application.port.out.RoomMemberQueryRepository;
import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomInvitation;
import com.daebangg.sayroam.room.domain.RoomInvitationRepository;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomMemberStatus;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomInvitationRepository roomInvitationRepository;
    private final RoomMemberQueryRepository roomMemberQueryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(Long userId, String name) {
        Room room = roomRepository.save(new Room(name));
        roomMemberRepository.save(RoomMember.joinAsOwner(room, userId, LocalDateTime.now(ZoneOffset.UTC)));
        return room.getId();
    }

    // 여행 방은 이름이 없다. 화면은 여행 이름을 보여 준다
    @Transactional
    public Long createForTrip() {
        return roomRepository.save(new Room(null)).getId();
    }

    @Transactional
    public void join(Long roomId, Long userId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .ifPresentOrElse(member -> member.rejoin(now),
                        () -> roomMemberRepository.save(RoomMember.join(roomRepository.getReferenceById(roomId), userId, now)));
    }

    @Transactional
    public String createInvitation(Long roomId, Long userId) {
        RoomMember member = getJoinedMember(roomId, userId);
        RoomInvitation invitation = member.createShareInvitation(LocalDateTime.now(ZoneOffset.UTC));
        return roomInvitationRepository.save(invitation).getCode();
    }

    @Transactional
    public Long joinByInvitation(String code, Long userId) {
        RoomInvitation invitation = roomInvitationRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_INVITATION_NOT_FOUND));
        Long roomId = invitation.getRoomId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .ifPresentOrElse(member -> member.rejoin(invitation, now),
                        () -> roomMemberRepository.save(RoomMember.joinAsMember(invitation, userId, now)));
        return roomId;
    }

    @Transactional
    public void leave(Long roomId, Long userId) {
        RoomMember member = getMember(roomId, userId);
        member.leave(LocalDateTime.now(ZoneOffset.UTC));
        eventPublisher.publishEvent(new RoomMemberLeft(roomId, userId));
    }

    public void ensureJoined(Long roomId, Long userId) {
        getJoinedMember(roomId, userId);
    }

    public long countJoinedMembers(Long roomId) {
        return roomMemberRepository.countByRoomIdAndStatus(roomId, RoomMemberStatus.JOINED);
    }

    public LocalDateTime getJoinedAt(Long roomId, Long userId) {
        return getJoinedMember(roomId, userId).getJoinedAt();
    }

    public List<RoomMemberProfileResult> findMembers(Long roomId, Long userId) {
        ensureJoined(roomId, userId);
        return roomMemberQueryRepository.findProfiles(roomId);
    }

    private RoomMember getMember(Long roomId, Long userId) {
        return roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    private RoomMember getJoinedMember(Long roomId, Long userId) {
        RoomMember member = getMember(roomId, userId);
        member.ensureJoined();
        return member;
    }
}
