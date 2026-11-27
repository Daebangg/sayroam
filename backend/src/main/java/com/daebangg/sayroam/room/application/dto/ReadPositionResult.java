package com.daebangg.sayroam.room.application.dto;

import com.daebangg.sayroam.room.domain.RoomMember;

import java.time.LocalDateTime;

// 안 읽은 메시지를 셀 기준. 아직 읽지 않았으면 lastReadMessageId는 null이다
public record ReadPositionResult(Long lastReadMessageId, LocalDateTime joinedAt) {

    public static ReadPositionResult from(RoomMember member) {
        return new ReadPositionResult(member.getLastReadMessageId(), member.getJoinedAt());
    }
}
