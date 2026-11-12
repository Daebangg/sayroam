package com.daebangg.sayroam.room.application;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomInvitationJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RecordApplicationEvents
class RoomServiceTest extends DatabaseIntegrationTest {

    @Autowired
    RoomService roomService;

    @Autowired
    ApplicationEvents events;

    @Autowired
    RoomJpaRepository roomRepository;

    @Autowired
    RoomMemberJpaRepository roomMemberRepository;

    @Autowired
    RoomInvitationJpaRepository invitationRepository;

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        invitationRepository.deleteAllInBatch();
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 방을_만들면_방과_방장이_함께_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long roomId = roomService.create(userId, "제주 3박 4일");

        // then
        assertThat(roomRepository.count()).isEqualTo(1);
        RoomMember owner = roomMemberRepository.findByRoomIdAndUserId(roomId, userId).orElseThrow();
        assertThat(owner.isOwner()).isTrue();
    }

    @Test
    void 방장_저장에_실패하면_방도_저장되지_않는다() {
        // given
        Long 없는_회원 = 999L;

        // when
        assertThatThrownBy(() -> roomService.create(없는_회원, "제주 3박 4일"))
                .isInstanceOf(DataIntegrityViolationException.class);

        // then
        assertThat(roomRepository.count()).isZero();
    }

    @Test
    void 방_멤버가_아니면_접근을_거부한다() {
        // given
        Long owner_id = saveUser();
        Long roomId = roomService.create(owner_id, "제주 3박 4일");
        Long noneMemberId = 999L;

        assertThatThrownBy(() -> roomService.ensureJoined(roomId, noneMemberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 참여_중인_멤버면_접근을_허용한다() {
        Long userId = saveUser();

        Long roomId = roomService.create(userId, "제주 3박 4일");

        assertThatCode(() -> roomService.ensureJoined(roomId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    void 없는_방이면_접근을_거부한다() {
        // given
        Long userId = saveUser();
        Long nonExistentRoomId = 999L;

        assertThatThrownBy(() -> roomService.ensureJoined(nonExistentRoomId, userId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 방_멤버는_초대_링크를_만들_수_있다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 여행");

        // when
        String code = roomService.createInvitation(roomId, userId);

        // then
        assertThat(invitationRepository.findByCode(code)).isPresent();
    }

    @Test
    void 방_멤버가_아니면_초대_링크를_만들_수_없다() {
        // given
        Long ownerId = saveUser();
        Long outsiderId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");

        // when & then
        assertThatThrownBy(() -> roomService.createInvitation(roomId, outsiderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 유효한_초대로_참여하면_일반_멤버로_저장되고_방_ID를_응답한다() {
        // given
        Long ownerId = saveUser();
        Long userId = saveUser();
        Long roomId = roomService.create(ownerId, "제주");
        String code = roomService.createInvitation(roomId, ownerId);

        Long joinedRoomId = roomService.joinByInvitation(code, userId);

        // then
        assertThat(joinedRoomId).isEqualTo(roomId);
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow();
        assertThat(member.isOwner()).isFalse();
    }

    @Test
    void 하나의_초대로_여러_명이_방_멤버가_된다() {
        // given
        Long ownerId = saveUser();
        Long firstId = saveUser();
        Long secondId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 여행");
        String code = roomService.createInvitation(roomId, ownerId);

        // when
        roomService.joinByInvitation(code, firstId);
        roomService.joinByInvitation(code, secondId);

        // then
        assertThat(roomMemberRepository.findByRoomIdAndUserId(roomId, firstId)).isPresent();
        assertThat(roomMemberRepository.findByRoomIdAndUserId(roomId, secondId)).isPresent();
    }

    @Test
    void 이미_참여_중인_회원은_멤버가_늘지_않고_방_ID를_응답한다() {
        // given
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 여행");
        String code = roomService.createInvitation(roomId, ownerId);

        // when
        Long joinedRoomId = roomService.joinByInvitation(code, ownerId);

        // then
        assertThat(joinedRoomId).isEqualTo(roomId);
        assertThat(roomMemberRepository.count()).isEqualTo(1L);

    }

    @Test
    void 없는_코드로는_참여할_수_없다() {
        Long userId = saveUser();

        assertThatThrownBy(() -> roomService.joinByInvitation("x", userId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_INVITATION_NOT_FOUND);
                        });
    }

    @Test
    void 나가면_방에_접근할_수_없다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주");

        // when
        roomService.leave(roomId, userId);

        // then
        assertThatThrownBy(() -> roomService.ensureJoined(roomId, userId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED);
                        });
    }

    @Test
    void 나가면_멤버_나감_이벤트를_발행한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");

        // when
        roomService.leave(roomId, userId);

        // then
        assertThat(events.stream(RoomMemberLeft.class))
                .containsExactly(new RoomMemberLeft(roomId, userId));
    }

    @Test
    void 이미_나간_멤버가_다시_나가면_이벤트를_발행하지_않는다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        roomService.leave(roomId, userId);
        events.clear();

        // when
        assertThatThrownBy(() -> roomService.leave(roomId, userId))
                .isInstanceOf(BusinessException.class);

        // then
        assertThat(events.stream(RoomMemberLeft.class)).isEmpty();
    }

    @Test
    void 참여하지_않은_방은_나갈_수_없다() {
        // given
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        Long outsiderId = saveUser();

        // when & then
        assertThatThrownBy(() -> roomService.leave(roomId, outsiderId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RoomErrorCode.ROOM_ACCESS_DENIED));
    }

    @Test
    void 나간_회원이_초대_링크로_다시_참여하면_방에_접근할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long userId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        String code = roomService.createInvitation(roomId, ownerId);
        roomService.joinByInvitation(code, userId);
        roomService.leave(roomId, userId);

        // when
        roomService.joinByInvitation(code, userId);

        // then
        assertThatCode(() -> roomService.ensureJoined(roomId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    void 참여중인_회원은_만료된_초대_링크로도_방_ID를_응답한다() {
        // given
        Long ownerId = saveUser();
        Long roomId = roomService.create(ownerId, "제주 3박 4일");
        String code = roomService.createInvitation(roomId, ownerId);
        expireInvitation(code);

        // when
        Long joinedRoomId = roomService.joinByInvitation(code, ownerId);

        // then
        assertThat(joinedRoomId).isEqualTo(roomId);
    }

    private Long saveUser() {
        User user = User.builder().nickname("방장").role(Role.USER).build();
        return userRepository.save(user).getId();
    }

    private void expireInvitation(String code) {
        transactionTemplate.executeWithoutResult(status -> entityManager
                .createQuery("UPDATE RoomInvitation i SET i.expiresAt = :past WHERE i.code = :code")
                .setParameter("past", LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .setParameter("code", code)
                .executeUpdate());
    }
}
