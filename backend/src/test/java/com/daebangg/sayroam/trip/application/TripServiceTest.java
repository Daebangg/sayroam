package com.daebangg.sayroam.trip.application;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import com.daebangg.sayroam.place.domain.Coordinate;
import com.daebangg.sayroam.place.domain.Place;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceName;
import com.daebangg.sayroam.place.domain.PlaceSource;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import com.daebangg.sayroam.place.infrastructure.persistence.PlaceJpaRepository;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomInvitationJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.application.dto.AddTripCustomPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.AddTripPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripPlace;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import com.daebangg.sayroam.trip.infrastructure.persistence.TripJpaRepository;
import com.daebangg.sayroam.trip.infrastructure.persistence.TripPlaceJpaRepository;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TripServiceTest extends DatabaseIntegrationTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);
    private static final String ROOM_NAME = "제주 여행";
    private static final String TITLE = "제주 여행";
    private static final Long MISSING_TRIP_ID = 999_999L;
    private static final String SOURCE_ID = "126081";

    @Autowired
    TripService tripService;

    @Autowired
    RoomService roomService;

    @Autowired
    TripJpaRepository tripJpaRepository;

    @Autowired
    PlaceJpaRepository placeJpaRepository;

    @Autowired
    TripPlaceJpaRepository tripPlaceJpaRepository;

    @MockitoBean
    PlaceCatalog placeCatalog;

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
        tripPlaceJpaRepository.deleteAllInBatch();
        placeJpaRepository.deleteAllInBatch();
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
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // then
        assertThat(tripJpaRepository.findById(tripId)).isPresent();
        assertThat(tripJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 제목을_붙여_만들면_앞뒤_공백을_지운_제목이_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, START_DATE, END_DATE, "  제주 여행 ");

        // then
        assertThat(tripJpaRepository.findById(tripId))
                .hasValueSatisfying(trip -> assertThat(trip.getTitle()).isEqualTo("제주 여행"));
    }

    @Test
    void 공백뿐인_제목으로_만들면_제목_없이_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, START_DATE, END_DATE, "   ");

        // then
        assertThat(tripJpaRepository.findById(tripId))
                .hasValueSatisfying(trip -> assertThat(trip.getTitle()).isNull());
    }

    @Test
    void 제목이_30자를_넘으면_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();
        String title = "가".repeat(31);

        // when
        assertThatThrownBy(() -> tripService.create(userId, START_DATE, END_DATE, title))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_TITLE_TOO_LONG));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 종료일이_시작일보다_앞서면_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();

        // when
        assertThatThrownBy(() -> tripService.create(userId, END_DATE, START_DATE, TITLE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 채팅방이_없는_여행에서_채팅방을_만들면_방장과_함께_만들어지고_여행에_연결된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

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
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);

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
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        List<Throwable> failures = runTwiceConcurrently(() -> tripService.createRoom(userId, tripId, ROOM_NAME));

        // then
        assertThat(failures).singleElement()
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_ROOM_ALREADY_LINKED));
        assertThat(roomJpaRepository.count()).isEqualTo(1);
    }

    private List<Throwable> runTwiceConcurrently(Callable<?> action) throws Exception {
        return runConcurrently(action, action);
    }

    private List<Throwable> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Object>> futures = List.of(
                    pool.submit(gate(first, ready, start)), pool.submit(gate(second, ready, start)));
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

    private Callable<Object> gate(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return action.call();
        };
    }

    @Test
    void 내가_만든_여행만_목록에_나온다() {
        // given
        Long userId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long myTripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        tripService.create(otherId, START_DATE, END_DATE, TITLE);

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(myTripId);
    }

    @Test
    void 목록은_나중에_만든_여행부터_나온다() {
        // given
        Long userId = saveUser();
        Long firstTripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        Long secondTripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long joinedTripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long joinedRoomId = tripService.createRoom(ownerId, joinedTripId, ROOM_NAME);
        Long otherTripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long joinedTripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long joinedRoomId = tripService.createRoom(ownerId, joinedTripId, ROOM_NAME);
        Long otherTripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
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
        Long tripId = tripService.create(userId, START_DATE, END_DATE, null);

        // when
        TripSummaryResult trip = tripService.find(userId, tripId);

        // then
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.period()).isEqualTo(new TravelPeriod(START_DATE, END_DATE));
        assertThat(trip.title()).isNull();
        assertThat(trip.roomId()).isNull();
    }

    @Test
    void 여행을_만든_회원은_채팅방이_없는_여행의_이름을_바꾼다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        TripSummaryResult result = tripService.rename(userId, tripId, "부산 여행");

        // then
        assertThat(result.title()).isEqualTo("부산 여행");
        assertThat(findTrip(tripId).getTitle()).isEqualTo("부산 여행");
    }

    @Test
    void 채팅방_멤버는_연결된_여행의_이름을_바꾼다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);

        // when
        tripService.rename(memberId, tripId, "부산 여행");

        // then
        assertThat(findTrip(tripId).getTitle()).isEqualTo("부산 여행");
    }

    @Test
    void 채팅방_생성과_이름_변경을_동시에_요청해도_채팅방_연결과_새_이름이_모두_남는다() throws Exception {
        // 두 요청의 실행 순서는 매번 달라 경쟁 상황을 여러 번 만든다
        for (int attempt = 0; attempt < 20; attempt++) {
            // given
            Long userId = saveUser("회원" + attempt);
            Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

            // when
            List<Throwable> failures = runConcurrently(
                    () -> tripService.createRoom(userId, tripId, ROOM_NAME),
                    () -> tripService.rename(userId, tripId, "부산 여행"));

            // then
            assertThat(failures).isEmpty();
            Trip trip = findTrip(tripId);
            assertThat(trip.getRoomId()).isNotNull();
            assertThat(trip.getTitle()).isEqualTo("부산 여행");
        }
    }

    @Test
    void 다른_회원의_여행_이름은_바꿀_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);

        // when
        assertThatThrownBy(() -> tripService.rename(otherId, tripId, "부산 여행"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));

        // then
        assertThat(findTrip(tripId).getTitle()).isEqualTo(TITLE);
    }

    @Test
    void 채팅방을_나간_멤버는_여행_이름을_바꿀_수_없다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        roomService.leave(roomId, memberId);

        // when
        assertThatThrownBy(() -> tripService.rename(memberId, tripId, "부산 여행"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));

        // then
        assertThat(findTrip(tripId).getTitle()).isEqualTo(TITLE);
    }

    @Test
    void 없는_여행의_이름은_바꿀_수_없다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.rename(userId, MISSING_TRIP_ID, "부산 여행"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 이름을_보내지_않으면_기존_이름을_유지한다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        TripSummaryResult result = tripService.rename(userId, tripId, null);

        // then
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(findTrip(tripId).getTitle()).isEqualTo(TITLE);
    }

    @Test
    void 빈_이름을_보내면_이름_없는_여행이_된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        tripService.rename(userId, tripId, "");

        // then
        assertThat(findTrip(tripId).getTitle()).isNull();
    }

    @Test
    void 다른_회원의_여행은_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);

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

    @Test
    void 장소를_추가하면_일차와_메모와_추가한_회원이_저장된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        givenTourPlace();

        // when
        Long tripPlaceId = tripService.addPlace(userId, tripId, command(2, "오전에 가기"));

        // then
        assertThat(tripPlaceJpaRepository.findById(tripPlaceId)).get()
                .extracting("dayNumber", "memo", "addedById")
                .containsExactly(2, "오전에 가기", userId);
    }

    @Test
    void 채팅방에_참여_중인_멤버는_장소를_추가할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        givenTourPlace();

        // when
        Long tripPlaceId = tripService.addPlace(memberId, tripId, command(null, null));

        // then
        assertThat(tripPlaceJpaRepository.findById(tripPlaceId)).get()
                .extracting("addedById")
                .isEqualTo(memberId);
    }

    @Test
    void 채팅방을_나간_멤버면_장소_추가를_TRIP_NOT_FOUND로_거부한다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        roomService.leave(roomId, memberId);
        givenTourPlace();

        // when & then
        assertThatThrownBy(() -> tripService.addPlace(memberId, tripId, command(null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
        assertThat(tripPlaceJpaRepository.count()).isZero();
    }

    @Test
    void 채팅방이_없는_여행에_다른_회원이_추가하면_TourAPI를_호출하지_않고_거부한다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        givenTourPlace();

        // when & then
        assertThatThrownBy(() -> tripService.addPlace(otherId, tripId, command(null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
        verify(placeCatalog, never()).find(any());
    }

    @Test
    void 없는_여행이면_장소_추가를_TRIP_NOT_FOUND로_거부한다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.addPlace(userId, MISSING_TRIP_ID, command(null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 일차가_기간을_벗어나면_TourAPI를_호출하지_않고_거부한다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        givenTourPlace();

        // when & then
        assertThatThrownBy(() -> tripService.addPlace(userId, tripId, command(5, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));
        verify(placeCatalog, never()).find(any());
    }

    @Test
    void TourAPI에서_찾을_수_없는_장소면_PLACE_NOT_FOUND로_거부하고_아무것도_저장하지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        given(placeCatalog.find(SOURCE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tripService.addPlace(userId, tripId, command(1, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND));
        assertThat(tripPlaceJpaRepository.count()).isZero();
        assertThat(placeJpaRepository.count()).isZero();
    }

    @Test
    void 같은_장소를_한_여행에_여러_번_추가할_수_있다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        givenTourPlace();

        // when
        tripService.addPlace(userId, tripId, command(1, null));
        tripService.addPlace(userId, tripId, command(3, null));

        // then
        assertThat(tripPlaceJpaRepository.count()).isEqualTo(2);
        assertThat(placeJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 직접_고른_위치와_이름으로_추가하면_일차와_메모와_추가한_회원이_저장된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        Long tripPlaceId = tripService.addCustomPlace(userId, tripId, customCommand(2, "체크인 4시"));

        // then
        assertThat(tripPlaceJpaRepository.findById(tripPlaceId)).get()
                .extracting("dayNumber", "memo", "addedById")
                .containsExactly(2, "체크인 4시", userId);
        assertThat(placeJpaRepository.countBySource(PlaceSource.CUSTOM)).isEqualTo(1);
    }

    @Test
    void 채팅방에_참여_중인_멤버는_장소를_직접_추가할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);

        // when
        Long tripPlaceId = tripService.addCustomPlace(memberId, tripId, customCommand(null, null));

        // then
        assertThat(tripPlaceJpaRepository.findById(tripPlaceId)).get()
                .extracting("addedById")
                .isEqualTo(memberId);
    }

    @Test
    void 다른_회원의_여행이면_직접_추가를_TRIP_NOT_FOUND로_거부하고_장소를_저장하지_않는다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);

        // when & then
        assertThatThrownBy(() -> tripService.addCustomPlace(otherId, tripId, customCommand(null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
        assertThat(placeJpaRepository.count()).isZero();
    }

    @Test
    void 일차가_기간을_벗어나면_직접_추가를_거부하고_장소를_저장하지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when & then
        assertThatThrownBy(() -> tripService.addCustomPlace(userId, tripId, customCommand(5, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));
        assertThat(placeJpaRepository.count()).isZero();
    }

    @Test
    void 좌표가_범위를_벗어나면_직접_추가를_거부하고_장소를_저장하지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        AddTripCustomPlaceCommand command = new AddTripCustomPlaceCommand(
                "숙소", new BigDecimal("91"), new BigDecimal("126.5311884"), null, null);

        // when & then
        assertThatThrownBy(() -> tripService.addCustomPlace(userId, tripId, command))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
        assertThat(placeJpaRepository.count()).isZero();
    }

    @Test
    void 메모가_너무_길면_거부하고_직접_추가한_장소도_남기지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        String memo = "가".repeat(TripPlace.MAX_MEMO_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> tripService.addCustomPlace(userId, tripId, customCommand(null, memo)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_PLACE_MEMO_TOO_LONG));
        assertThat(placeJpaRepository.count()).isZero();
        assertThat(tripPlaceJpaRepository.count()).isZero();
    }

    @Test
    void 담긴_장소가_없으면_빈_목록을_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).isEmpty();
    }

    @Test
    void 없는_여행이면_장소_목록_조회를_TRIP_NOT_FOUND로_거부한다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.findPlaces(userId, MISSING_TRIP_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 다른_회원의_여행이면_장소_목록_조회를_TRIP_NOT_FOUND로_거부한다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);

        // when & then
        assertThatThrownBy(() -> tripService.findPlaces(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 장소마다_이름_좌표_일차_메모_추가한_회원을_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        Long tripPlaceId = tripService.addCustomPlace(userId, tripId, customCommand(2, "체크인 4시"));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).singleElement()
                .extracting(TripPlaceResult::tripPlaceId, TripPlaceResult::name, TripPlaceResult::latitude,
                        TripPlaceResult::longitude, TripPlaceResult::day, TripPlaceResult::memo,
                        TripPlaceResult::addedById)
                .containsExactly(tripPlaceId, "숙소", new BigDecimal("33.4996213"), new BigDecimal("126.5311884"),
                        2, "체크인 4시", userId);
    }

    @Test
    void 검색으로_추가한_장소는_출처를_TOUR로_분류와_주소를_함께_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        givenTourPlace();
        tripService.addPlace(userId, tripId, command(1, null));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).singleElement()
                .extracting(TripPlaceResult::source, TripPlaceResult::name, TripPlaceResult::category,
                        TripPlaceResult::address)
                .containsExactly(PlaceSource.TOUR, "송정해수욕장", PlaceCategory.ATTRACTION, "부산광역시 해운대구 송정해변로 62");
    }

    @Test
    void 직접_추가한_장소는_출처를_CUSTOM으로_분류와_주소_없이_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        tripService.addCustomPlace(userId, tripId, customCommand(null, null));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).singleElement()
                .extracting(TripPlaceResult::source, TripPlaceResult::category, TripPlaceResult::address)
                .containsExactly(PlaceSource.CUSTOM, null, null);
    }

    @Test
    void 담긴_장소를_일차_순서대로_돌려주고_일차_미정은_마지막에_둔다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        tripService.addCustomPlace(userId, tripId, customCommand(null, "미정"));
        tripService.addCustomPlace(userId, tripId, customCommand(3, "3일차"));
        tripService.addCustomPlace(userId, tripId, customCommand(1, "1일차"));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).extracting(TripPlaceResult::memo)
                .containsExactly("1일차", "3일차", "미정");
    }

    @Test
    void 같은_일차의_장소는_추가한_순서대로_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        tripService.addCustomPlace(userId, tripId, customCommand(2, "처음"));
        tripService.addCustomPlace(userId, tripId, customCommand(2, "두 번째"));
        tripService.addCustomPlace(userId, tripId, customCommand(2, "세 번째"));
        tripService.addCustomPlace(userId, tripId, customCommand(null, "미정 처음"));
        tripService.addCustomPlace(userId, tripId, customCommand(null, "미정 두 번째"));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).extracting(TripPlaceResult::memo)
                .containsExactly("처음", "두 번째", "세 번째", "미정 처음", "미정 두 번째");
    }

    @Test
    void 다른_여행에_담긴_장소는_포함하지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        Long otherTripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        tripService.addCustomPlace(userId, tripId, customCommand(1, "이 여행"));
        tripService.addCustomPlace(userId, otherTripId, customCommand(1, "다른 여행"));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(userId, tripId);

        // then
        assertThat(places).extracting(TripPlaceResult::memo).containsExactly("이 여행");
    }

    @Test
    void 채팅방에_참여_중인_멤버는_장소_목록을_조회할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        tripService.addCustomPlace(ownerId, tripId, customCommand(1, null));

        // when
        List<TripPlaceResult> places = tripService.findPlaces(memberId, tripId);

        // then
        assertThat(places).singleElement()
                .extracting(TripPlaceResult::addedById)
                .isEqualTo(ownerId);
    }

    @Test
    void 채팅방을_나간_멤버면_장소_목록_조회를_TRIP_NOT_FOUND로_거부한다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE, TITLE);
        Long roomId = tripService.createRoom(ownerId, tripId, ROOM_NAME);
        joinRoom(roomId, ownerId, memberId);
        roomService.leave(roomId, memberId);

        // when & then
        assertThatThrownBy(() -> tripService.findPlaces(memberId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 장소_목록을_조회할_때_TourAPI를_호출하지_않는다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE, TITLE);
        givenTourPlace();
        tripService.addPlace(userId, tripId, command(1, null));
        clearInvocations(placeCatalog);

        // when
        tripService.findPlaces(userId, tripId);

        // then
        verifyNoInteractions(placeCatalog);
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

    // 실제 어댑터처럼 조회할 때마다 새 장소 객체를 돌려준다
    private void givenTourPlace() {
        given(placeCatalog.find(SOURCE_ID)).willAnswer(invocation -> Optional.of(Place.tour(
                SOURCE_ID, new PlaceName("송정해수욕장"), PlaceCategory.ATTRACTION, "12",
                "부산광역시 해운대구 송정해변로 62",
                new Coordinate(new BigDecimal("35.1786"), new BigDecimal("129.1997")))));
    }

    private AddTripPlaceCommand command(Integer day, String memo) {
        return new AddTripPlaceCommand(SOURCE_ID, day, memo);
    }

    private AddTripCustomPlaceCommand customCommand(Integer day, String memo) {
        return new AddTripCustomPlaceCommand(
                "숙소", new BigDecimal("33.4996213"), new BigDecimal("126.5311884"), day, memo);
    }
}
