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
        RoomMember owner = RoomMember.joinAsOwner(room, 1L, NOW);

        // then
        assertThat(owner.isOwner()).isTrue();
    }

    @Test
    void 참여_중인_멤버면_방_접근을_허용한다() {
        // given
        RoomMember member = RoomMember.joinAsOwner(new Room("제주 3박 4일"), 1L, NOW);

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
    void 초대로_참여하면_참여_시각이_기록된다() {
        // given
        RoomInvitation invitation = invitationIssuedAt(NOW);

        // when
        RoomMember member = RoomMember.joinAsMember(invitation, 2L, NOW);

        // then
        assertThat(member.getJoinedAt()).isEqualTo(NOW);
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

    @Test
    void 나간_멤버가_유효한_초대로_참여하면_방에_다시_참여할_수_있다() {
        // given
        RoomMember member = leftMember();
        LocalDateTime rejoinedAt = NOW.plusDays(3);
        RoomInvitation invitation = invitationIssuedAt(rejoinedAt);

        // when
        member.rejoin(invitation, rejoinedAt);

        // then
        assertThatCode(member::ensureJoined)
                .doesNotThrowAnyException();
        assertThat(member.getJoinedAt()).isEqualTo(rejoinedAt);
    }

    @Test
    void 나간_멤버가_만료된_초대로_참여하면_거부한다() {
        // given
        RoomMember member = leftMember();
        RoomInvitation expired = invitationIssuedAt(NOW.minusYears(1));

        // when & then
        assertThatThrownBy(() -> member.rejoin(expired, NOW))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_INVITATION_EXPIRED));
    }

    @Test
    void 참여_중인_멤버가_다시_참여해도_참여_시각은_그대로다() {
        // given
        RoomMember member = joinedMember();
        LocalDateTime now = NOW.plusDays(3);

        // when
        member.rejoin(invitationIssuedAt(now), now);

        // then
        assertThat(member.getJoinedAt()).isEqualTo(NOW);
    }

    @Test
    void 나간_멤버는_다시_나갈_수_없다() {
        // given
        RoomMember member = leftMember();

        // when & then
        assertThatThrownBy(() -> member.leave(NOW.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    private RoomInvitation invitationIssuedAt(LocalDateTime issuedAt) {
        return RoomMember.joinAsOwner(room, 1L, NOW).createShareInvitation(issuedAt);
    }

    private RoomMember joinedMember() {
        return RoomMember.joinAsMember(invitationIssuedAt(NOW), 2L, NOW);
    }

    private RoomMember leftMember() {
        RoomMember member = joinedMember();
        member.leave(NOW);
        return member;
    }
}
