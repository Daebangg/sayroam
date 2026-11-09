package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomMemberTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 9, 12, 0);
    private final Room room = new Room("제주 3박 4일");

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
    void 참여_중인_멤버면_방_접근을_허용한다() {
        // given
        RoomMember member = RoomMember.joinAsOwner(new Room("제주 3박 4일"), 1L);

        assertThatCode(member::ensureJoined)
                .doesNotThrowAnyException();
    }

    @Test
    void 참여_중인_멤버가_나가면_방_접근이_거부된다() {
        // given
        RoomMember member = joinedMember();

        // when
        member.leave(NOW);

        // then
        assertThatThrownBy(member::ensureJoined)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 유효한_초대로_참여하면_방에_접근할_수_있다() {
        // given
        RoomInvitation invitation = invitationIssuedAt(NOW);

        // when
        RoomMember member = RoomMember.joinAsMember(invitation, 2L, NOW);

        // then
        assertThatCode(member::ensureJoined)
                .doesNotThrowAnyException();
    }

    @Test
    void 만료된_초대로는_참여할_수_없다() {
        // given
        RoomInvitation expired = invitationIssuedAt(NOW.minusYears(1));

        // when & then
        assertThatThrownBy(() -> RoomMember.joinAsMember(expired, 2L, NOW))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_INVITATION_EXPIRED));
    }

    @Test
    void 하나의_초대로_여러_명이_참여할_수_있다() {
        // given
        RoomInvitation invitation = invitationIssuedAt(NOW);

        // when
        RoomMember first = RoomMember.joinAsMember(invitation, 2L, NOW);
        RoomMember second = RoomMember.joinAsMember(invitation, 3L, NOW);

        // then
        assertThatCode(() -> {
            first.ensureJoined();
            second.ensureJoined();
        }).doesNotThrowAnyException();
    }

    private RoomInvitation invitationIssuedAt(LocalDateTime issuedAt) {
        return RoomMember.joinAsOwner(room, 1L).createShareInvitation(issuedAt);
    }

    private RoomMember joinedMember() {
        return RoomMember.joinAsMember(invitationIssuedAt(NOW), 2L, NOW);
    }
}
