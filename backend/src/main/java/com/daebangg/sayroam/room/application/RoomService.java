package com.daebangg.sayroam.room.application;

import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public Long create(Long userId, String name) {
        Room room = roomRepository.save(new Room(name));
        roomMemberRepository.save(RoomMember.joinAsOwner(room, userId));
        return room.getId();
    }

}
