package com.daebangg.sayroam.trip.application;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.destination.infrastructure.persistence.DestinationJpaRepository;
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
import com.daebangg.sayroam.room.infrastructure.persistence.RoomJpaRepository;
import com.daebangg.sayroam.room.infrastructure.persistence.RoomMemberJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.application.dto.AddTripCustomPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.AddTripPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.CreateTripCommand;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.dto.TripTitleResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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
    private static final Long MISSING_DESTINATION_ID = Long.MAX_VALUE;
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
    DestinationJpaRepository destinationJpaRepository;

    @Autowired
    JdbcTemplate jdbc;

    Long destinationId;

    @BeforeEach
    void setUpDestination() {
        destinationId = destinationJpaRepository.findActiveByName("제주").getFirst().getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("UPDATE destination SET is_active = 1");
        tripPlaceJpaRepository.deleteAllInBatch();
        placeJpaRepository.deleteAllInBatch();
        tripJpaRepository.deleteAllInBatch();
        roomMemberJpaRepository.deleteAllInBatch();
        roomJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 여행을_만들면_채팅방도_만들어_연결한다() {
        // given
        Long ownerId = saveUser();

        // when
        Long tripId = createTrip(ownerId);

        // then
        Long roomId = findTrip(tripId).getRoomId();
        assertThat(roomId).isNotNull();
        assertThat(roomJpaRepository.findById(roomId)).isPresent();
    }

    @Test
    void 여행을_만들면_만든_회원이_채팅방의_유일한_멤버다() {
        // given
        Long ownerId = saveUser();

        // when
        Long tripId = createTrip(ownerId);

        // then
        Long roomId = findTrip(tripId).getRoomId();
        assertThatCode(() -> roomService.ensureJoined(roomId, ownerId)).doesNotThrowAnyException();
        assertThat(roomService.countJoinedMembers(roomId)).isEqualTo(1);
    }

    @Test
    void 여행마다_서로_다른_채팅방이_만들어진다() {
        // given
        Long ownerId = saveUser();

        // when
        Long firstTripId = createTrip(ownerId);
        Long secondTripId = createTrip(ownerId);

        // then
        assertThat(findTrip(firstTripId).getRoomId()).isNotEqualTo(findTrip(secondTripId).getRoomId());
    }

    @Test
    void 비활성_여행지로는_여행도_채팅방도_만들지_않는다() {
        // given
        Long ownerId = saveUser();
        jdbc.update("UPDATE destination SET is_active = 0 WHERE id = ?", destinationId);

        // when & then
        assertThatThrownBy(() -> createTrip(ownerId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DESTINATION_UNAVAILABLE));
        assertThat(tripJpaRepository.count()).isZero();
        assertThat(roomJpaRepository.count()).isZero();
    }

    @Test
    void 채팅방을_저장한_뒤_멤버_저장이_실패하면_여행도_채팅방도_남지_않는다() {
        // given
        Long missingUserId = 999_999L;

        // when & then
        assertThatThrownBy(() -> createTrip(missingUserId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(tripJpaRepository.count()).isZero();
        assertThat(roomJpaRepository.count()).isZero();
        assertThat(roomMemberJpaRepository.count()).isZero();
    }

    @Test
    void 여행을_만들면_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // then
        assertThat(tripJpaRepository.findById(tripId)).isPresent();
        assertThat(tripJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 제목을_붙여_만들면_앞뒤_공백을_지운_제목이_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, "  제주 여행 "));

        // then
        assertThat(tripJpaRepository.findById(tripId))
                .hasValueSatisfying(trip -> assertThat(trip.getTitle()).isEqualTo("제주 여행"));
    }

    @Test
    void 공백뿐인_제목으로_만들면_제목_없이_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, "   "));

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
        assertThatThrownBy(() -> tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, title)))
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
        assertThatThrownBy(() -> tripService.create(userId, new CreateTripCommand(destinationId, END_DATE, START_DATE, TITLE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 목적지가_없으면_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();

        // when
        assertThatThrownBy(() -> tripService.create(userId,
                new CreateTripCommand(null, START_DATE, END_DATE, TITLE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DESTINATION_REQUIRED));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 목록에_없는_여행지로는_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();

        // when
        assertThatThrownBy(() -> tripService.create(userId,
                new CreateTripCommand(MISSING_DESTINATION_ID, START_DATE, END_DATE, TITLE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DESTINATION_UNAVAILABLE));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 비활성_여행지로는_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();
        jdbc.update("UPDATE destination SET is_active = 0 WHERE id = ?", destinationId);

        // when
        assertThatThrownBy(() -> tripService.create(userId,
                new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DESTINATION_UNAVAILABLE));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 여행을_만든_회원이_초대하면_초대_코드를_준다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);

        // when
        String code = tripService.issueInvitation(ownerId, tripId);

        // then
        assertThat(code).isNotBlank();
    }

    @Test
    void 유효한_초대_코드가_있으면_같은_코드를_돌려준다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        String first = tripService.issueInvitation(ownerId, tripId);

        // when
        String second = tripService.issueInvitation(ownerId, tripId);

        // then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void 초대_코드가_만료됐으면_새_코드를_만든다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        String first = tripService.issueInvitation(ownerId, tripId);
        expireInvitation(tripId);

        // when
        String second = tripService.issueInvitation(ownerId, tripId);

        // then
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void 함께하는_여행의_멤버도_초대할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);

        // when
        String code = tripService.issueInvitation(memberId, tripId);

        // then
        assertThat(code).isNotBlank();
    }

    @Test
    void 볼_수_없는_여행에는_초대할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = createTrip(ownerId);

        // when & then
        assertThatThrownBy(() -> tripService.issueInvitation(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 발급하기_전에는_초대_코드가_없다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);

        // when
        Optional<String> code = tripService.findInvitation(ownerId, tripId);

        // then
        assertThat(code).isEmpty();
    }

    @Test
    void 발급한_초대_코드를_조회할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        String issued = tripService.issueInvitation(ownerId, tripId);

        // when
        Optional<String> code = tripService.findInvitation(ownerId, tripId);

        // then
        assertThat(code).contains(issued);
    }

    @Test
    void 만료된_초대는_조회되지_않는다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        tripService.issueInvitation(ownerId, tripId);
        expireInvitation(tripId);

        // when
        Optional<String> code = tripService.findInvitation(ownerId, tripId);

        // then
        assertThat(code).isEmpty();
    }

    @Test
    void 만료된_초대를_조회해도_새_코드를_만들지_않는다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        String issued = tripService.issueInvitation(ownerId, tripId);
        expireInvitation(tripId);

        // when
        tripService.findInvitation(ownerId, tripId);

        // then
        assertThat(jdbc.queryForObject("SELECT invitation_code FROM trip WHERE id = ?", String.class, tripId))
                .isEqualTo(issued);
    }

    @Test
    void 함께하는_여행의_멤버도_초대_코드를_조회할_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
        String issued = tripService.issueInvitation(ownerId, tripId);

        // when
        Optional<String> code = tripService.findInvitation(memberId, tripId);

        // then
        assertThat(code).contains(issued);
    }

    @Test
    void 볼_수_없는_여행의_초대는_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = createTrip(ownerId);
        tripService.issueInvitation(ownerId, tripId);

        // when & then
        assertThatThrownBy(() -> tripService.findInvitation(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 초대_링크로_참여하면_여행_채팅방의_멤버가_되고_여행_ID를_받는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);

        // when
        Long joinedTripId = tripService.join(memberId, code);

        // then
        assertThat(joinedTripId).isEqualTo(tripId);
        Long roomId = findTrip(tripId).getRoomId();
        assertThatCode(() -> roomService.ensureJoined(roomId, memberId)).doesNotThrowAnyException();
        assertThat(roomService.countJoinedMembers(roomId)).isEqualTo(2);
    }

    @Test
    void 초대_링크로_참여하면_그_여행을_목록과_상세에서_볼_수_있다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);

        // when
        tripService.join(memberId, code);

        // then
        assertThat(tripService.findMine(memberId)).extracting(TripSummaryResult::tripId).containsExactly(tripId);
        assertThat(tripService.find(memberId, tripId).tripId()).isEqualTo(tripId);
    }

    @Test
    void 이미_참여_중인_회원이_다시_참여해도_멤버가_늘지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);
        tripService.join(memberId, code);

        // when
        Long joinedTripId = tripService.join(memberId, code);

        // then
        assertThat(joinedTripId).isEqualTo(tripId);
        assertThat(roomService.countJoinedMembers(findTrip(tripId).getRoomId())).isEqualTo(2);
    }

    @Test
    void 여행을_만든_회원이_자기_초대로_참여해도_멤버가_늘지_않는다() {
        // given
        Long ownerId = saveUser();
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);

        // when
        Long joinedTripId = tripService.join(ownerId, code);

        // then
        assertThat(joinedTripId).isEqualTo(tripId);
        assertThat(roomService.countJoinedMembers(findTrip(tripId).getRoomId())).isEqualTo(1);
    }

    @Test
    void 나갔던_멤버가_다시_참여하면_여행_채팅방에_돌아온다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);
        tripService.join(memberId, code);
        Long roomId = findTrip(tripId).getRoomId();
        roomService.leave(roomId, memberId);

        // when
        tripService.join(memberId, code);

        // then
        assertThatCode(() -> roomService.ensureJoined(roomId, memberId)).doesNotThrowAnyException();
    }

    @Test
    void 없는_초대_코드로는_참여할_수_없다() {
        // given
        Long memberId = saveUser("친구");

        // when & then
        assertThatThrownBy(() -> tripService.join(memberId, "missing-code"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_INVITATION_NOT_FOUND));
    }

    @Test
    void 만료된_초대_코드로는_참여할_수_없고_멤버도_늘지_않는다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("친구");
        Long tripId = createTrip(ownerId);
        String code = tripService.issueInvitation(ownerId, tripId);
        expireInvitation(tripId);

        // when & then
        assertThatThrownBy(() -> tripService.join(memberId, code))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_INVITATION_NOT_FOUND));
        assertThat(roomService.countJoinedMembers(findTrip(tripId).getRoomId())).isEqualTo(1);
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
        Long myTripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        tripService.create(otherId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(myTripId);
    }

    @Test
    void 목록은_나중에_만든_여행부터_나온다() {
        // given
        Long userId = saveUser();
        Long firstTripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long secondTripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(secondTripId, firstTripId);
    }

    @Test
    void 여행_목록에도_목적지를_돌려준다() {
        // given
        Long userId = saveUser();
        tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).singleElement()
                .satisfies(trip -> assertThat(trip.destinationName()).isEqualTo("제주"));
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
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
        Long joinedTripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long joinedRoomId = findTrip(joinedTripId).getRoomId();
        Long otherTripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        joinRoom(joinedRoomId, memberId);

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
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
        Long joinedTripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long joinedRoomId = findTrip(joinedTripId).getRoomId();
        Long otherTripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        joinRoom(joinedRoomId, memberId);

        // when & then
        assertThatThrownBy(() -> tripService.find(memberId, otherTripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 내가_만든_여행을_조회하면_기간과_비어_있는_제목과_채팅방_ID를_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, null));

        // when
        TripSummaryResult trip = tripService.find(userId, tripId);

        // then
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.period()).isEqualTo(new TravelPeriod(START_DATE, END_DATE));
        assertThat(trip.title()).isNull();
        assertThat(trip.roomId()).isEqualTo(findTrip(tripId).getRoomId()).isNotNull();
    }

    @Test
    void 여행을_조회하면_고른_목적지의_이름과_지도_영역을_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        TripSummaryResult trip = tripService.find(userId, tripId);

        // then
        assertThat(trip.destinationId()).isEqualTo(destinationId);
        assertThat(trip.destinationName()).isEqualTo("제주");
        assertThat(trip.boundsSouth()).isLessThan(trip.boundsNorth());
        assertThat(trip.boundsWest()).isLessThan(trip.boundsEast());
    }

    @Test
    void 여행을_만든_회원은_채팅방이_없는_여행의_이름을_바꾼다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        TripTitleResult result = tripService.rename(userId, tripId, "부산 여행");

        // then
        assertThat(result.title()).isEqualTo("부산 여행");
        assertThat(findTrip(tripId).getTitle()).isEqualTo("부산 여행");
    }

    @Test
    void 채팅방_멤버는_연결된_여행의_이름을_바꾼다() {
        // given
        Long ownerId = saveUser();
        Long memberId = saveUser("멤버");
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);

        // when
        tripService.rename(memberId, tripId, "부산 여행");

        // then
        assertThat(findTrip(tripId).getTitle()).isEqualTo("부산 여행");
    }

    @Test
    void 다른_회원의_여행_이름은_바꿀_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when
        TripTitleResult result = tripService.rename(userId, tripId, null);

        // then
        assertThat(result.title()).isEqualTo(TITLE);
        assertThat(findTrip(tripId).getTitle()).isEqualTo(TITLE);
    }

    @Test
    void 빈_이름을_보내면_이름_없는_여행이_된다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));

        // when & then
        assertThatThrownBy(() -> tripService.findPlaces(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 장소마다_이름_좌표_일차_메모_추가한_회원을_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long otherTripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
        Long roomId = findTrip(tripId).getRoomId();
        joinRoom(roomId, memberId);
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
        Long tripId = tripService.create(userId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
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

    private Long createTrip(Long ownerId) {
        return tripService.create(ownerId, new CreateTripCommand(destinationId, START_DATE, END_DATE, TITLE));
    }

    private void expireInvitation(Long tripId) {
        jdbc.update("UPDATE trip SET invitation_expires_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(1), tripId);
    }

    private Trip findTrip(Long tripId) {
        return tripJpaRepository.findById(tripId).orElseThrow();
    }

    private void joinRoom(Long roomId, Long memberId) {
        roomService.join(roomId, memberId);
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
