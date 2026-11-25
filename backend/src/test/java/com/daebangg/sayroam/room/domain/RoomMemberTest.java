package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
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
    void 나간_멤버는_다시_나갈_수_없다() {
        // given
        RoomMember member = leftMember();

        // when & then
        assertThatThrownBy(() -> member.leave(NOW.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 방에_참여하면_방에_접근할_수_있다() {
        // when
        RoomMember member = RoomMember.join(room, 2L, NOW);

        // then
        assertThatCode(member::ensureJoined).doesNotThrowAnyException();
    }

    @Test
    void 방에_참여하면_참여_시각이_기록된다() {
        // when
        RoomMember member = RoomMember.join(room, 2L, NOW);

        // then
        assertThat(member.getJoinedAt()).isEqualTo(NOW);
    }

    @Test
    void 나간_멤버가_다시_참여하면_방에_접근할_수_있고_참여_시각이_바뀐다() {
        // given
        RoomMember member = RoomMember.join(room, 2L, NOW);
        member.leave(NOW.plusDays(1));
        LocalDateTime rejoinedAt = NOW.plusDays(3);

        // when
        member.rejoin(rejoinedAt);

        // then
        assertThatCode(member::ensureJoined).doesNotThrowAnyException();
        assertThat(member.getJoinedAt()).isEqualTo(rejoinedAt);
    }

    @Test
    void 참여_중인_멤버를_다시_참여시켜도_참여_시각은_그대로다() {
        // given
        RoomMember member = RoomMember.join(room, 2L, NOW);

        // when
        member.rejoin(NOW.plusDays(3));

        // then
        assertThat(member.getJoinedAt()).isEqualTo(NOW);
    }

    private RoomMember joinedMember() {
        return RoomMember.join(room, 2L, NOW);
    }

    private RoomMember leftMember() {
        RoomMember member = joinedMember();
        member.leave(NOW);
        return member;
    }
}
