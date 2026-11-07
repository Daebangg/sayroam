package com.daebangg.sayroam.room.application;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomMemberStatus;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.type.Role;
import com.daebangg.sayroam.user.domain.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceTest extends DatabaseIntegrationTest {

    @Autowired
    RoomService roomService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomMemberRepository roomMemberRepository;

    // TODO: UserRepository 머지 후 회원 저장·정리를 userRepository로 교체
    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createQuery("DELETE FROM User").executeUpdate());
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
    void 나간_멤버면_접근을_거부한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.create(userId, "제주 3박 4일");
        markMemberAsLeft(roomId, userId);

        assertThatThrownBy(() -> roomService.ensureJoined(roomId, userId))
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

    private Long saveUser() {
        return transactionTemplate.execute(status -> {
            User user = User.builder().nickname("방장").role(Role.USER).build();
            entityManager.persist(user);
            return (Long) entityManagerFactory.getPersistenceUnitUtil().getIdentifier(user);
        });
    }

    private void markMemberAsLeft(Long roomId, Long userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId).orElseThrow();
        ReflectionTestUtils.setField(member, "status", RoomMemberStatus.LEFT);
        roomMemberRepository.save(member);
    }
}
