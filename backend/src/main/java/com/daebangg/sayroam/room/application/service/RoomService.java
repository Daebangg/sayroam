package com.daebangg.sayroam.room.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.dto.ReadPositionResult;
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
        return roomRepository.save(Room.createForTrip()).getId();
    }

    @Transactional
    public Long createForCompanion() {
        return roomRepository.save(Room.createForCompanion()).getId();
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

    // 탈퇴할 때 부른다. 화면 갱신은 UserWithdrawn이, 연결 종료는 LoginsInvalidated가 맡으므로 RoomMemberLeft는 보내지 않는다
    @Transactional
    public void leaveAll(Long userId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        roomMemberRepository.findAllByUserIdAndStatus(userId, RoomMemberStatus.JOINED)
                .forEach(member -> member.leave(now));
    }

    @Transactional
    public void advanceLastRead(Long roomId, Long userId, Long messageId) {
        roomMemberRepository.advanceLastRead(roomId, userId, messageId);
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

    public ReadPositionResult getReadPosition(Long roomId, Long userId) {
        return ReadPositionResult.from(getJoinedMember(roomId, userId));
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
