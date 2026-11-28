package com.daebangg.sayroam.room.application.port.out;


public interface RoomMemberChangeBroadcaster {

    void broadcast(Long roomId);
}
