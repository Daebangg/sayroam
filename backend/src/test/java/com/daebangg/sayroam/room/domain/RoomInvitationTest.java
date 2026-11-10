package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomInvitationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Test
    void 초대_링크를_만들면_초대_코드가_발급된다() {
        // given
        RoomMember member = createMember();

        // when
        RoomInvitation invitation = member.createShareInvitation(NOW);

        // then
        assertThat(invitation.getCode()).isNotBlank();
    }

    @Test
    void 만든_지_7일_이하면_유효하다() {
        // given
        RoomInvitation invitation = createInvitation();
        LocalDateTime now = NOW.plusDays(7);

        // when & then
        assertThatCode(() -> invitation.ensureValid(now))
                .doesNotThrowAnyException();
    }

    @Test
    void 만든_지_7일이_지나면_만료된다() {
        // given
        RoomInvitation invitation = createInvitation();
        LocalDateTime now = NOW.plusDays(7).plusNanos(1);

        // when & then
        assertThatThrownBy(() -> invitation.ensureValid(now))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_INVITATION_EXPIRED);
    }

    private RoomMember createMember() {
        return RoomMember.joinAsOwner(new Room("제주 여행"), 1L, NOW);
    }

    private RoomInvitation createInvitation() {
        return createMember().createShareInvitation(NOW);
    }
}
