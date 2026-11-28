package com.daebangg.sayroam.room.application.listener;

import com.daebangg.sayroam.room.application.port.out.RoomMemberChangeBroadcaster;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.event.UserProfileChanged;
import com.daebangg.sayroam.user.domain.event.UserWithdrawn;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;

class RoomMemberChangeListenerTest extends DatabaseIntegrationTest {

    @MockitoBean
    RoomMemberChangeBroadcaster broadcaster;

    @Autowired
    RoomService roomService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    RoomJpaRepository roomRepository;

    @Autowired
    RoomMemberJpaRepository roomMemberRepository;

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        roomMemberRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 참여가_커밋되면_그_방에_멤버_변화를_알린다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.createForTrip();

        // when
        roomService.join(roomId, userId);

        // then
        then(broadcaster).should().broadcast(roomId);
    }

    @Test
    void 나감이_커밋되면_그_방에_멤버_변화를_알린다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.createForTrip();
        roomService.join(roomId, userId);
        clearInvocations(broadcaster);

        // when
        roomService.leave(roomId, userId);

        // then
        then(broadcaster).should().broadcast(roomId);
    }

    @Test
    void 참여가_롤백되면_알리지_않는다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.createForTrip();

        // when
        transactionTemplate.executeWithoutResult(status -> {
            roomService.join(roomId, userId);
            status.setRollbackOnly();
        });

        // then
        then(broadcaster).shouldHaveNoInteractions();
    }

    @Test
    void 프로필이_바뀌면_나간_방을_포함해_회원이_속했던_방에_알린다() {
        // given
        Long userId = saveUser();
        Long otherId = saveUser();
        Long joinedRoomId = roomService.createForTrip();
        Long leftRoomId = roomService.createForTrip();
        Long otherRoomId = roomService.createForTrip();
        roomService.join(joinedRoomId, userId);
        roomService.join(leftRoomId, userId);
        roomService.leave(leftRoomId, userId);
        roomService.join(otherRoomId, otherId);
        clearInvocations(broadcaster);

        // when
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new UserProfileChanged(userId)));

        // then
        then(broadcaster).should().broadcast(joinedRoomId);
        then(broadcaster).should().broadcast(leftRoomId);
        then(broadcaster).shouldHaveNoMoreInteractions();
    }

    @Test
    void 탈퇴하면_회원이_속했던_방에_알린다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.createForTrip();
        roomService.join(roomId, userId);
        clearInvocations(broadcaster);

        // when
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new UserWithdrawn(userId)));

        // then
        then(broadcaster).should().broadcast(roomId);
    }

    @Test
    void 한_방에_알리지_못해도_나머지_방에는_알린다() {
        // given
        Long userId = saveUser();
        Long failingRoomId = roomService.createForTrip();
        Long roomId = roomService.createForTrip();
        roomService.join(failingRoomId, userId);
        roomService.join(roomId, userId);
        clearInvocations(broadcaster);
        willThrow(new MessageDeliveryException("broker down")).given(broadcaster).broadcast(failingRoomId);

        // when
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new UserProfileChanged(userId)));

        // then
        then(broadcaster).should().broadcast(roomId);
    }

    @Test
    void 알림에_실패해도_참여는_성공한다() {
        // given
        Long userId = saveUser();
        Long roomId = roomService.createForTrip();
        willThrow(new MessageDeliveryException("broker down")).given(broadcaster).broadcast(roomId);

        // when
        roomService.join(roomId, userId);

        // then
        assertThat(roomService.countJoinedMembers(roomId)).isEqualTo(1);
    }

    private Long saveUser() {
        User user = User.builder().nickname("여행자").role(Role.USER).build();
        return userRepository.save(user).getId();
    }
}
