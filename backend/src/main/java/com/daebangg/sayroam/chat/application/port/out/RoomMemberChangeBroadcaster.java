package com.daebangg.sayroam.chat.application.port.out;


public interface RoomMemberChangeBroadcaster {

    void broadcast(Long roomId);
}
