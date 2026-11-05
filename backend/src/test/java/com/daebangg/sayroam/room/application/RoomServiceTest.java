package com.daebangg.sayroam.room.application;

import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import com.daebangg.sayroam.room.domain.RoomRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
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
        Long userId = 회원을_저장한다();

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

    private Long 회원을_저장한다() {
        return transactionTemplate.execute(status -> {
            User user = User.builder().nickname("방장").role(Role.USER).build();
            entityManager.persist(user);
            return (Long) entityManagerFactory.getPersistenceUnitUtil().getIdentifier(user);
        });
    }
}
