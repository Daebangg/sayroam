package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void 나간_멤버면_방_접근을_거부한다() {
        // given
        RoomMember member = RoomMember.joinAsOwner(new Room("제주 3박 4일"), 1L);

        ReflectionTestUtils.setField(member, "status", RoomMemberStatus.LEFT);

        assertThatThrownBy(member::ensureJoined)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));

    }

    @Test
    void 참여_중인_멤버면_방_접근을_허용한다() {
        // given
        RoomMember member = RoomMember.joinAsOwner(new Room("제주 3박 4일"), 1L);

        assertThatCode(member::ensureJoined)
                .doesNotThrowAnyException();
    }

}