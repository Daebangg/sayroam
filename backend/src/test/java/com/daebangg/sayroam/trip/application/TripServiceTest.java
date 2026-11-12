package com.daebangg.sayroam.trip.application;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomInvitationJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import com.daebangg.sayroam.trip.infrastructure.persistence.TripJpaRepository;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripServiceTest extends DatabaseIntegrationTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);
    private static final String ROOM_NAME = "제주 여행";
    private static final Long MISSING_TRIP_ID = 999_999L;

    @Autowired
    TripService tripService;

    @Autowired
    RoomService roomService;

    @Autowired
    TripJpaRepository tripJpaRepository;

    @Autowired
    UserJpaRepository userRepository;

    @Autowired
    RoomJpaRepository roomJpaRepository;

    @Autowired
    RoomMemberJpaRepository roomMemberJpaRepository;

    @Autowired
    RoomInvitationJpaRepository roomInvitationJpaRepository;

    @AfterEach
    void cleanUp() {
        tripJpaRepository.deleteAllInBatch();
        roomInvitationJpaRepository.deleteAllInBatch();
        roomMemberJpaRepository.deleteAllInBatch();
        roomJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 여행을_만들면_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // then
        assertThat(tripJpaRepository.findById(tripId)).isPresent();
        assertThat(tripJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 종료일이_시작일보다_앞서면_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();

        // when
        assertThatThrownBy(() -> tripService.create(userId, END_DATE, START_DATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 채팅방이_없는_여행에서_채팅방을_만들면_방장과_함께_만들어지고_여행에_연결된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        Long roomId = tripService.createRoom(userId, tripId, ROOM_NAME);

        // then
        assertThat(roomJpaRepository.count()).isEqualTo(1);
        RoomMember owner = roomMemberJpaRepository.findByRoomIdAndUserId(roomId, userId).orElseThrow();
        assertThat(owner.isOwner()).isTrue();
        assertThat(findTrip(tripId).getRoomId()).isEqualTo(roomId);
    }

    @Test
    void 이미_채팅방이_연결된_여행이면_409로_거부하고_새_채팅방을_만들지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(userId, tripId, ROOM_NAME);

        // when & then
        assertThatThrownBy(() -> tripService.createRoom(userId, tripId, ROOM_NAME))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_ROOM_ALREADY_LINKED));
        assertThat(roomJpaRepository.count()).isEqualTo(1);
        assertThat(findTrip(tripId).getRoomId()).isEqualTo(roomId);
    }

    @Test
    void 다른_회원의_여행에는_채팅방을_만들_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser();
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);

        // when & then

        assertThatThrownBy(() -> tripService.createRoom(otherId, tripId, ROOM_NAME))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
        assertThat(roomJpaRepository.count()).isZero();
        assertThat(findTrip(tripId).getRoomId()).isNull();
    }

    @Test
    void 없는_여행에는_채팅방을_만들_수_없다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.createRoom(userId, MISSING_TRIP_ID, ROOM_NAME))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
        assertThat(roomJpaRepository.count()).isZero();
    }

    @Test
    void 같은_여행으로_동시에_요청하면_채팅방은_하나만_생긴다() throws Exception {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        List<Throwable> failures = runTwiceConcurrently(() -> tripService.createRoom(userId, tripId, ROOM_NAME));

        // then
        assertThat(failures).singleElement()
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_ROOM_ALREADY_LINKED));
        assertThat(roomJpaRepository.count()).isEqualTo(1);
    }

    private List<Throwable> runTwiceConcurrently(Callable<?> action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Object> gated = () -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return action.call();
            };
            List<Future<Object>> futures = List.of(pool.submit(gated), pool.submit(gated));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> failures = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    future.get(20, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        }
    }

    @Test
    void 내가_만든_여행만_목록에_나온다() {
        // given
        Long userId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long myTripId = tripService.create(userId, START_DATE, END_DATE);
        tripService.create(otherId, START_DATE, END_DATE);

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(myTripId);
    }

    @Test
    void 목록은_나중에_만든_여행부터_나온다() {
        // given
        Long userId = saveUser();
        Long firstTripId = tripService.create(userId, START_DATE, END_DATE);
        Long secondTripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(secondTripId, firstTripId);
    }

    @Test
    void 만든_여행이_없으면_빈_목록을_돌려준다() {
        // given
        Long userId = saveUser();

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).isEmpty();
    }

    @Test
    void 참여_중인_채팅방에_연결된_여행도_목록에_나온다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);

        // when
        List<TripSummaryResult> trips = tripService.findMine(memberId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId)
                .containsExactly(tripId);
    }

    @Test
    void 채팅방을_나간_멤버의_여행_목록에는_여행이_나오지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser();
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        roomService.leave(roomId, memberId);

        // when
        List<TripSummaryResult> trips = tripService.findMine(memberId);

        // then
        assertThat(trips).isEmpty();
    }

    @Test
    void 여행을_만든_회원도_채팅방을_나가면_목록에_그_여행이_나오지_않는다() {
        // given
        Long ownerId = saveUser();
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        roomService.leave(roomId, ownerId);

        // when
        List<TripSummaryResult> trips = tripService.findMine(ownerId);

        // then
        assertThat(trips).isEmpty();
    }

    @Test
    void 다른_채팅방에만_참여한_회원의_목록에는_그_여행이_나오지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long joinedTripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long joinedRoomId = tripService.createRoom(ownerId, joinedTripId, ROOM_NAME);
        Long otherTripId = tripService.create(ownerId, START_DATE, END_DATE);
        tripService.createRoom(ownerId, otherTripId, ROOM_NAME);
        joinRoom(joinedRoomId, ownerId, memberId);

        // when
        List<TripSummaryResult> trips = tripService.findMine(memberId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(joinedTripId);
    }

    @Test
    void 참여_중인_채팅방에_연결된_여행을_조회할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);

        // when
        TripSummaryResult trip = tripService.find(memberId, tripId);

        // then
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.roomId()).isEqualTo(roomId);
    }

    @Test
    void 채팅방을_나간_멤버는_여행을_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        roomService.leave(roomId, memberId);

        // when & then
        assertThatThrownBy(() -> tripService.find(memberId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 만든_회원도_채팅방을_나가면_여행을_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        roomService.leave(roomId, ownerId);

        // when & then
        assertThatThrownBy(() -> tripService.find(ownerId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 다른_채팅방에만_참여한_회원은_그_여행을_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long joinedTripId = tripService.create(ownerId, START_DATE, END_DATE);
        Long joinedRoomId = tripService.createRoom(ownerId, joinedTripId, ROOM_NAME);
        Long otherTripId = tripService.create(ownerId, START_DATE, END_DATE);
        tripService.createRoom(ownerId, otherTripId, ROOM_NAME);
        joinRoom(joinedRoomId, ownerId, memberId);

        // when & then
        assertThatThrownBy(() -> tripService.find(memberId, otherTripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 내가_만든_여행을_조회하면_기간과_비어_있는_제목과_채팅방_ID를_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        TripSummaryResult trip = tripService.find(userId, tripId);

        // then
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.period()).isEqualTo(new TravelPeriod(START_DATE, END_DATE));
        assertThat(trip.title()).isNull();
        assertThat(trip.roomId()).isNull();
    }

    @Test
    void 다른_회원의_여행은_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);

        // when & then
        assertThatThrownBy(() -> tripService.find(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 없는_여행은_조회할_수_없다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.find(userId, MISSING_TRIP_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    private Long saveUser() {
        return saveUser("여행자");
    }

    private Long saveUser(String nickname) {
        User user = User.builder().nickname(nickname).role(Role.USER).build();
        return userRepository.save(user).getId();
    }

    private Trip findTrip(Long tripId) {
        return tripJpaRepository.findById(tripId).orElseThrow();
    }

    private void joinRoom(Long roomId, Long ownerId, Long memberId) {
        String invitationCode = roomService.createInvitation(roomId, ownerId);
        roomService.joinByInvitation(invitationCode, memberId);
    }
}
