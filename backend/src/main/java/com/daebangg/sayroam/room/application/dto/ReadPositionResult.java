package com.daebangg.sayroam.room.application.dto;

import com.daebangg.sayroam.room.domain.RoomMember;

import java.time.LocalDateTime;

public record ReadPositionResult(Long lastReadMessageId, LocalDateTime joinedAt) {

    public static ReadPositionResult from(RoomMember member) {
        return new ReadPositionResult(member.getLastReadMessageId(), member.getJoinedAt());
    }
}
