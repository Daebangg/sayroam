package com.daebangg.sayroam.room.application.port.out;

import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;

import java.util.List;

public interface RoomMemberQueryRepository {
    // 권한은 확인하지 않는다. 호출 전에 요청한 회원이 방에 참여 중인지 확인한다.
    List<RoomMemberProfileResult> findProfiles(Long roomId);
}
