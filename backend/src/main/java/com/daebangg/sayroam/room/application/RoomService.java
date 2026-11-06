package com.daebangg.sayroam.room.application;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    @Transactional
    public Long create(Long userId, String name) {
        Room room = roomRepository.save(new Room(name));
        roomMemberRepository.save(RoomMember.joinAsOwner(room, userId));
        return room.getId();
    }

    public void ensureJoined(Long roomId, Long userId) {
        getJoinedMember(roomId, userId);
    }

    public LocalDateTime getJoinedAt(Long roomId, Long userId) {
        return getJoinedMember(roomId, userId).getJoinedAt();
    }

    private RoomMember getJoinedMember(Long roomId, Long userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED));
        member.ensureJoined();
        return member;
    }

}
