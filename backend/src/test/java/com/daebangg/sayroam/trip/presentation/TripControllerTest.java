package com.daebangg.sayroam.trip.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceSource;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import com.daebangg.sayroam.trip.application.dto.AddTripCustomPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.AddTripPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.CreateTripCommand;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.dto.TripTitleResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import com.daebangg.sayroam.trip.presentation.controller.TripController;
import com.daebangg.sayroam.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class TripControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 12L;
    private static final Long DESTINATION_ID = 3L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);
    private static final Long OTHER_TRIP_ID = 13L;
    private static final Long ROOM_ID = 7L;
    private static final String INVITATION_CODE = "k3Xv9qLm2Tz8RwYb7NfA";
    private static final String SOURCE_ID = "126081";
    private static final Long TRIP_PLACE_ID = 31L;
    private static final BigDecimal LATITUDE = new BigDecimal("33.4996213");
    private static final BigDecimal LONGITUDE = new BigDecimal("126.5311884");
    private static final TravelPeriod PERIOD = new TravelPeriod(START_DATE, END_DATE);
    private static final BigDecimal SOUTH = new BigDecimal("33.113712");
    private static final BigDecimal WEST = new BigDecimal("126.144116");
    private static final BigDecimal NORTH = new BigDecimal("33.565734");
    private static final BigDecimal EAST = new BigDecimal("126.970428");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TripService tripService;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void 로그인한_회원의_기간으로_여행을_만들고_201과_여행_ID를_응답한다() throws Exception {
        // given
        given(tripService.create(USER_ID, new CreateTripCommand(DESTINATION_ID, START_DATE, END_DATE, null))).willReturn(TRIP_ID);

        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID));
    }

    @Test
    void 여행_생성_요청의_제목을_서비스에_전달한다() throws Exception {
        // given
        given(tripService.create(USER_ID, new CreateTripCommand(DESTINATION_ID, START_DATE, END_DATE, "제주 여행"))).willReturn(TRIP_ID);

        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01", "endDate": "2026-12-04", "title": "제주 여행"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID));
    }

    @Test
    void 제목이_30자를_넘으면_400과_에러_코드를_응답한다() throws Exception {
        // given
        String title = "가".repeat(31);
        given(tripService.create(USER_ID, new CreateTripCommand(DESTINATION_ID, START_DATE, END_DATE, title)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_TITLE_TOO_LONG));

        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01", "endDate": "2026-12-04", "title": "%s"}
                        """.formatted(title)).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRIP_TITLE_TOO_LONG"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 시작일이_없으면_400과_에러_코드를_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 종료일이_없으면_400과_에러_코드를_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("endDate"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 목적지가_없으면_400과_에러_코드를_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("destinationId"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 목록에_없거나_비활성인_여행지면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.create(USER_ID, new CreateTripCommand(DESTINATION_ID, START_DATE, END_DATE, null)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_DESTINATION_UNAVAILABLE));

        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRIP_DESTINATION_UNAVAILABLE"));
    }

    @Test
    void 종료일이_시작일보다_앞서면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.create(USER_ID, new CreateTripCommand(DESTINATION_ID, END_DATE, START_DATE, null)))
                .willThrow(new BusinessException(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026-12-04", "endDate": "2026-12-01"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRAVEL_PERIOD_END_BEFORE_START"));
    }

    @Test
    void 날짜_형식이_잘못되면_400을_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"destinationId": 3, "startDate": "2026/12/01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isBadRequest());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 여행_이름을_바꾸면_200과_여행_ID와_저장된_이름만_응답한다() throws Exception {
        // given
        given(tripService.rename(USER_ID, TRIP_ID, "부산 여행"))
                .willReturn(new TripTitleResult(TRIP_ID, "부산 여행"));

        // when & then
        mvc.perform(updateTripRequest(TRIP_ID, """
                        {"title": "부산 여행"}
                        """).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID))
                .andExpect(jsonPath("$.title").value("부산 여행"))
                .andExpect(jsonPath("$.startDate").doesNotExist())
                .andExpect(jsonPath("$.destination").doesNotExist());
    }

    @Test
    void 이름을_보내지_않으면_서비스에_null을_전달한다() throws Exception {
        // given
        given(tripService.rename(USER_ID, TRIP_ID, null))
                .willReturn(new TripTitleResult(TRIP_ID, "제주 여행"));

        // when & then
        mvc.perform(updateTripRequest(TRIP_ID, "{}").with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제주 여행"));
    }

    @Test
    void 수정할_수_없는_여행이면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.rename(USER_ID, TRIP_ID, "부산 여행"))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(updateTripRequest(TRIP_ID, """
                        {"title": "부산 여행"}
                        """).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 바꿀_이름이_30자를_넘으면_400과_에러_코드를_응답한다() throws Exception {
        // given
        String title = "가".repeat(31);
        given(tripService.rename(USER_ID, TRIP_ID, title))
                .willThrow(new BusinessException(TripErrorCode.TRIP_TITLE_TOO_LONG));

        // when & then
        mvc.perform(updateTripRequest(TRIP_ID, """
                        {"title": "%s"}
                        """.formatted(title)).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRIP_TITLE_TOO_LONG"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_여행을_수정하지_않는다() throws Exception {
        // when & then
        mvc.perform(updateTripRequest(TRIP_ID, """
                        {"title": "부산 여행"}
                        """))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 초대_링크를_발급하면_200과_초대_코드를_응답한다() throws Exception {
        // given
        given(tripService.issueInvitation(USER_ID, TRIP_ID)).willReturn(INVITATION_CODE);

        // when & then
        mvc.perform(issueInvitationRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(INVITATION_CODE));
    }

    @Test
    void 볼_수_없는_여행이면_초대_링크_발급에_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.issueInvitation(USER_ID, TRIP_ID))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(issueInvitationRequest(TRIP_ID).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_초대_링크를_발급하지_않는다() throws Exception {
        // when & then
        mvc.perform(issueInvitationRequest(TRIP_ID))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 초대_코드를_조회하면_200과_코드를_응답한다() throws Exception {
        // given
        given(tripService.findInvitation(USER_ID, TRIP_ID)).willReturn(Optional.of(INVITATION_CODE));

        // when & then
        mvc.perform(findInvitationRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(INVITATION_CODE));
    }

    @Test
    void 유효한_초대가_없으면_200과_빈_코드를_응답한다() throws Exception {
        // given
        given(tripService.findInvitation(USER_ID, TRIP_ID)).willReturn(Optional.empty());

        // when & then
        mvc.perform(findInvitationRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isEmpty());
    }

    @Test
    void 볼_수_없는_여행이면_초대_조회에_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.findInvitation(USER_ID, TRIP_ID))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(findInvitationRequest(TRIP_ID).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_초대_조회에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(findInvitationRequest(TRIP_ID))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 초대_코드로_참여하면_200과_여행_ID를_응답한다() throws Exception {
        // given
        given(tripService.join(USER_ID, INVITATION_CODE)).willReturn(TRIP_ID);

        // when & then
        mvc.perform(joinTripRequest("""
                        {"invitationCode": "%s"}
                        """.formatted(INVITATION_CODE)).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID));
    }

    @Test
    void 없거나_만료된_초대_코드면_참여에_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.join(USER_ID, INVITATION_CODE))
                .willThrow(new BusinessException(TripErrorCode.TRIP_INVITATION_NOT_FOUND));

        // when & then
        mvc.perform(joinTripRequest("""
                        {"invitationCode": "%s"}
                        """.formatted(INVITATION_CODE)).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_INVITATION_NOT_FOUND"));
    }

    @Test
    void 초대_코드_없이_참여하면_400과_에러_코드를_응답한다() throws Exception {
        // when & then
        mvc.perform(joinTripRequest("{}").with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 로그인하지_않으면_참여에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(joinTripRequest("""
                        {"invitationCode": "%s"}
                        """.formatted(INVITATION_CODE)))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 여행에_장소를_추가하면_201과_여행_장소_ID를_응답한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, 2, "오전에 가기")))
                .willReturn(TRIP_PLACE_ID);

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081", "day": 2, "memo": "오전에 가기"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripPlaceId").value(TRIP_PLACE_ID));
    }

    @Test
    void 일차와_메모_없이_장소_ID만_보내도_장소를_추가한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, null, null)))
                .willReturn(TRIP_PLACE_ID);

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripPlaceId").value(TRIP_PLACE_ID));
    }

    @Test
    void 장소_ID가_없으면_400과_에러_코드를_응답하고_장소를_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"day": 2}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("sourceId"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 일차가_1보다_작으면_400과_에러_코드를_응답하고_장소를_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081", "day": 0}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("day"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 메모가_255자를_넘으면_400과_에러_코드를_응답하고_장소를_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081", "memo": "%s"}
                        """.formatted("가".repeat(256))).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("memo"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 일차가_여행_기간을_벗어나면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, 5, null)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081", "day": 5}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRIP_DAY_OUT_OF_RANGE"));
    }

    @Test
    void 장소를_추가할_수_없는_여행이면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, null, null)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081"}
                        """).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void TourAPI에서_찾을_수_없는_장소면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, null, null)))
                .willThrow(new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND));

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081"}
                        """).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void TourAPI_조회에_실패하면_502와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addPlace(USER_ID, TRIP_ID, new AddTripPlaceCommand(SOURCE_ID, null, null)))
                .willThrow(new BusinessException(PlaceErrorCode.PLACE_SEARCH_FAILED));

        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081"}
                        """).with(login()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PLACE_SEARCH_FAILED"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_장소를_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripPlaceRequest(TRIP_ID, """
                        {"sourceId": "126081"}
                        """))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 지도에서_고른_장소를_직접_추가하면_201과_여행_장소_ID를_응답한다() throws Exception {
        // given
        given(tripService.addCustomPlace(USER_ID, TRIP_ID,
                new AddTripCustomPlaceCommand("숙소", LATITUDE, LONGITUDE, 2, "체크인 4시")))
                .willReturn(TRIP_PLACE_ID);

        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884, "day": 2, "memo": "체크인 4시"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripPlaceId").value(TRIP_PLACE_ID));
    }

    @Test
    void 일차와_메모_없이_이름과_좌표만_보내도_장소를_직접_추가한다() throws Exception {
        // given
        given(tripService.addCustomPlace(USER_ID, TRIP_ID,
                new AddTripCustomPlaceCommand("숙소", LATITUDE, LONGITUDE, null, null)))
                .willReturn(TRIP_PLACE_ID);

        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripPlaceId").value(TRIP_PLACE_ID));
    }

    @Test
    void 이름이_비어_있으면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": " ", "latitude": 33.4996213, "longitude": 126.5311884}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 이름이_100자를_넘으면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "%s", "latitude": 33.4996213, "longitude": 126.5311884}
                        """.formatted("가".repeat(101))).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 위도가_없으면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "longitude": 126.5311884}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("latitude"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 경도가_없으면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("longitude"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 위도가_범위를_벗어나면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 90.0000001, "longitude": 126.5311884}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("latitude"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 경도가_범위를_벗어나면_400과_에러_코드를_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": -180.0000001}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("longitude"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 직접_추가할_때_일차가_1보다_작으면_400과_에러_코드를_응답한다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884, "day": 0}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("day"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 직접_추가할_때_메모가_255자를_넘으면_400과_에러_코드를_응답한다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884, "memo": "%s"}
                        """.formatted("가".repeat(256))).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("memo"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 직접_추가할_때_일차가_여행_기간을_벗어나면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addCustomPlace(USER_ID, TRIP_ID,
                new AddTripCustomPlaceCommand("숙소", LATITUDE, LONGITUDE, 5, null)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));

        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884, "day": 5}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRIP_DAY_OUT_OF_RANGE"));
    }

    @Test
    void 장소를_직접_추가할_수_없는_여행이면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.addCustomPlace(USER_ID, TRIP_ID,
                new AddTripCustomPlaceCommand("숙소", LATITUDE, LONGITUDE, null, null)))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884}
                        """).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_장소를_직접_추가하지_않는다() throws Exception {
        // when & then
        mvc.perform(addTripCustomPlaceRequest(TRIP_ID, """
                        {"name": "숙소", "latitude": 33.4996213, "longitude": 126.5311884}
                        """))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 내_여행_목록을_조회하면_200과_여행_목록을_응답한다() throws Exception {
        // given
        given(tripService.findMine(USER_ID)).willReturn(List.of(
                new TripSummaryResult(OTHER_TRIP_ID, "제주 3박 4일", PERIOD, ROOM_ID, DESTINATION_ID, "제주", SOUTH, WEST, NORTH, EAST),
                new TripSummaryResult(TRIP_ID, null, PERIOD, null, DESTINATION_ID, "제주", SOUTH, WEST, NORTH, EAST)));

        // when & then
        mvc.perform(listTripsRequest().with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips.length()").value(2))
                .andExpect(jsonPath("$.trips[0].tripId").value(OTHER_TRIP_ID))
                .andExpect(jsonPath("$.trips[0].title").value("제주 3박 4일"))
                .andExpect(jsonPath("$.trips[0].roomId").value(ROOM_ID))
                .andExpect(jsonPath("$.trips[1].tripId").value(TRIP_ID))
                .andExpect(jsonPath("$.trips[1].title").isEmpty())
                .andExpect(jsonPath("$.trips[1].roomId").isEmpty());
    }

    @Test
    void 여행을_조회하면_200과_목적지_이름과_지도_영역을_포함한_여행을_응답한다() throws Exception {
        // given
        given(tripService.find(USER_ID, TRIP_ID)).willReturn(new TripSummaryResult(TRIP_ID, null, PERIOD, null, DESTINATION_ID, "제주", SOUTH, WEST, NORTH, EAST));

        // when & then
        mvc.perform(getTripRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID))
                .andExpect(jsonPath("$.startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-04"))
                .andExpect(jsonPath("$.title").isEmpty())
                .andExpect(jsonPath("$.roomId").isEmpty())
                .andExpect(jsonPath("$.destinationId").value(DESTINATION_ID))
                .andExpect(jsonPath("$.destinationName").value("제주"))
                .andExpect(jsonPath("$.boundsSouth").value(SOUTH.doubleValue()))
                .andExpect(jsonPath("$.boundsWest").value(WEST.doubleValue()))
                .andExpect(jsonPath("$.boundsNorth").value(NORTH.doubleValue()))
                .andExpect(jsonPath("$.boundsEast").value(EAST.doubleValue()));
    }

    @Test
    void 조회할_수_없는_여행이면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.find(USER_ID, TRIP_ID)).willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(getTripRequest(TRIP_ID).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_목록_조회에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(listTripsRequest())
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 로그인하지_않으면_단건_조회에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(getTripRequest(TRIP_ID))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 장소_목록을_조회하면_200과_장소_목록을_응답한다() throws Exception {
        // given
        given(tripService.findPlaces(USER_ID, TRIP_ID)).willReturn(List.of(
                new TripPlaceResult(31L, PlaceSource.TOUR, "송정해수욕장", PlaceCategory.ATTRACTION,
                        "부산광역시 해운대구 송정해변로 62", new BigDecimal("35.1786"), new BigDecimal("129.1997"),
                        1, "오전에 가기", USER_ID),
                new TripPlaceResult(32L, PlaceSource.CUSTOM, "숙소", null, null,
                        LATITUDE, LONGITUDE, null, null, 2L)));

        // when & then
        mvc.perform(listTripPlacesRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].tripPlaceId").value(31))
                .andExpect(jsonPath("$.places[0].source").value("TOUR"))
                .andExpect(jsonPath("$.places[0].name").value("송정해수욕장"))
                .andExpect(jsonPath("$.places[0].category").value("ATTRACTION"))
                .andExpect(jsonPath("$.places[0].address").value("부산광역시 해운대구 송정해변로 62"))
                .andExpect(jsonPath("$.places[0].latitude").value(35.1786))
                .andExpect(jsonPath("$.places[0].longitude").value(129.1997))
                .andExpect(jsonPath("$.places[0].day").value(1))
                .andExpect(jsonPath("$.places[0].memo").value("오전에 가기"))
                .andExpect(jsonPath("$.places[0].addedById").value(USER_ID))
                .andExpect(jsonPath("$.places[1].source").value("CUSTOM"))
                .andExpect(jsonPath("$.places[1].category").isEmpty())
                .andExpect(jsonPath("$.places[1].address").isEmpty())
                .andExpect(jsonPath("$.places[1].day").isEmpty())
                .andExpect(jsonPath("$.places[1].memo").isEmpty());
    }

    @Test
    void 담긴_장소가_없으면_빈_배열을_응답한다() throws Exception {
        // given
        given(tripService.findPlaces(USER_ID, TRIP_ID)).willReturn(List.of());

        // when & then
        mvc.perform(listTripPlacesRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places").isArray())
                .andExpect(jsonPath("$.places").isEmpty());
    }

    @Test
    void 장소_목록을_조회할_수_없는_여행이면_404와_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.findPlaces(USER_ID, TRIP_ID))
                .willThrow(new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        // when & then
        mvc.perform(listTripPlacesRequest(TRIP_ID).with(login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_장소_목록_조회에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(listTripPlacesRequest(TRIP_ID))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    private MockHttpServletRequestBuilder createTripRequest(String body) {
        return post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder updateTripRequest(Long tripId, String body) {
        return patch("/api/trips/{tripId}", tripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder joinTripRequest(String body) {
        return post("/api/trip-members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder findInvitationRequest(Long tripId) {
        return get("/api/trips/{tripId}/invitation", tripId);
    }

    private MockHttpServletRequestBuilder issueInvitationRequest(Long tripId) {
        return post("/api/trips/{tripId}/invitation", tripId);
    }

    private MockHttpServletRequestBuilder addTripPlaceRequest(Long tripId, String body) {
        return post("/api/trips/{tripId}/places", tripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder addTripCustomPlaceRequest(Long tripId, String body) {
        return post("/api/trips/{tripId}/custom-places", tripId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder listTripsRequest() {
        return get("/api/trips");
    }

    private MockHttpServletRequestBuilder getTripRequest(Long tripId) {
        return get("/api/trips/{tripId}", tripId);
    }

    private MockHttpServletRequestBuilder listTripPlacesRequest(Long tripId) {
        return get("/api/trips/{tripId}/places", tripId);
    }

    private RequestPostProcessor login() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "여행자", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
