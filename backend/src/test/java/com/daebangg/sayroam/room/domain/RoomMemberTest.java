package com.daebangg.sayroam.room.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomMemberTest {
    @Test
    void 방을_만든_회원은_방장이다() {
        // given
        Room room = new Room("제주 3박 4일");

        
        // when
        RoomMember owner = RoomMember.joinAsOwner(room, 1L);

        // then
        assertThat(owner.isOwner()).isTrue();
    }


}