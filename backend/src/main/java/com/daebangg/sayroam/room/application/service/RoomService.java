package com.daebangg.sayroam.room.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;
import com.daebangg.sayroam.room.application.port.out.RoomMemberQueryRepository;
import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomMemberStatus;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.domain.event.RoomMemberJoined;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomMemberQueryRepository roomMemberQueryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createForTrip() {
        return roomRepository.save(new Room()).getId();
    }

    @Transactional
    public void join(Long roomId, Long userId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Optional<RoomMember> member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        if (member.isEmpty()) {
            roomMemberRepository.save(RoomMember.join(roomRepository.getReferenceById(roomId), userId, now));
            eventPublisher.publishEvent(new RoomMemberJoined(roomId, userId));
            return;
        }
        if (member.get().rejoin(now)) {
            eventPublisher.publishEvent(new RoomMemberJoined(roomId, userId));
        }
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

    public List<Long> findRoomIds(Long userId) {
        return roomMemberQueryRepository.findRoomIdsByUserId(userId);
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
