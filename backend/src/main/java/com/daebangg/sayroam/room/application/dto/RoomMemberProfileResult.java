package com.daebangg.sayroam.room.application.dto;

import com.daebangg.sayroam.room.domain.RoomMemberStatus;

public record RoomMemberProfileResult(Long userId, String nickname, String profileImageUrl,
                                      RoomMemberStatus status) {
}
