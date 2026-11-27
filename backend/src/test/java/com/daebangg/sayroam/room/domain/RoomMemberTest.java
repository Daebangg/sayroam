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
    private final Room room = new Room();

    @Test
    void 참여_중인_멤버면_방_접근을_허용한다() {
        // given
        RoomMember member = RoomMember.join(new Room(), 1L, NOW);

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

    @Test
    void 나간_멤버가_다시_참여하면_참여_상태가_바뀌었다고_알린다() {
        // given
        RoomMember member = RoomMember.join(room, 1L, NOW);
        member.leave(NOW.plusHours(1));

        // when
        boolean changed = member.rejoin(NOW.plusHours(2));

        // then
        assertThat(changed).isTrue();
    }

    @Test
    void 참여_중인_멤버를_다시_참여시키면_바뀐_것이_없다고_알린다() {
        // given
        RoomMember member = RoomMember.join(room, 1L, NOW);

        // when
        boolean changed = member.rejoin(NOW.plusHours(1));

        // then
        assertThat(changed).isFalse();
    }

    @Test
    void 처음_읽으면_읽은_위치를_기록한다() {
        // given
        RoomMember member = joinedMember();

        // when
        member.advanceLastRead(120L);

        // then
        assertThat(member.getLastReadMessageId()).isEqualTo(120L);
    }

    @Test
    void 더_뒤의_메시지를_읽으면_읽은_위치를_옮긴다() {
        // given
        RoomMember member = joinedMember();
        member.advanceLastRead(120L);

        // when
        member.advanceLastRead(150L);

        // then
        assertThat(member.getLastReadMessageId()).isEqualTo(150L);
    }

    @Test
    void 앞선_메시지를_읽어도_읽은_위치를_되돌리지_않는다() {
        // given
        RoomMember member = joinedMember();
        member.advanceLastRead(150L);

        // when
        member.advanceLastRead(120L);

        // then
        assertThat(member.getLastReadMessageId()).isEqualTo(150L);
    }

    @Test
    void 나간_멤버는_읽은_위치를_옮길_수_없다() {
        // given
        RoomMember member = leftMember();

        // when & then
        assertThatThrownBy(() -> member.advanceLastRead(120L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
        assertThat(member.getLastReadMessageId()).isNull();
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
