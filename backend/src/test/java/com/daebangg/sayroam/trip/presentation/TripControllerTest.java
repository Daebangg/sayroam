package com.daebangg.sayroam.trip.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class TripControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long TRIP_ID = 12L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);
    private static final Long OTHER_TRIP_ID = 13L;
    private static final Long ROOM_ID = 7L;
    private static final TravelPeriod PERIOD = new TravelPeriod(START_DATE, END_DATE);

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
        given(tripService.create(USER_ID, START_DATE, END_DATE)).willReturn(TRIP_ID);

        // when & then
        mvc.perform(createTripRequest("""
                        {"startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"startDate": "2026-12-01", "endDate": "2026-12-04"}
                        """))
                .andExpect(status().isUnauthorized());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 시작일이_없으면_400과_에러_코드를_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"endDate": "2026-12-04"}
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
                        {"startDate": "2026-12-01"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("endDate"));

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 종료일이_시작일보다_앞서면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(tripService.create(USER_ID, END_DATE, START_DATE))
                .willThrow(new BusinessException(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // when & then
        mvc.perform(createTripRequest("""
                        {"startDate": "2026-12-04", "endDate": "2026-12-01"}
                        """).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRAVEL_PERIOD_END_BEFORE_START"));
    }

    @Test
    void 날짜_형식이_잘못되면_400을_응답하고_여행을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(createTripRequest("""
                        {"startDate": "2026/12/01", "endDate": "2026-12-04"}
                        """).with(login()))
                .andExpect(status().isBadRequest());

        then(tripService).shouldHaveNoInteractions();
    }

    @Test
    void 내_여행_목록을_조회하면_200과_여행_목록을_응답한다() throws Exception {
        // given
        given(tripService.findMine(USER_ID)).willReturn(List.of(
                new TripSummaryResult(OTHER_TRIP_ID, "제주 3박 4일", PERIOD, ROOM_ID),
                new TripSummaryResult(TRIP_ID, null, PERIOD, null)));

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
    void 여행을_조회하면_200과_여행을_응답한다() throws Exception {
        // given
        given(tripService.find(USER_ID, TRIP_ID)).willReturn(new TripSummaryResult(TRIP_ID, null, PERIOD, null));

        // when & then
        mvc.perform(getTripRequest(TRIP_ID).with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(TRIP_ID))
                .andExpect(jsonPath("$.startDate").value("2026-12-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-04"))
                .andExpect(jsonPath("$.title").isEmpty())
                .andExpect(jsonPath("$.roomId").isEmpty());
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

    private MockHttpServletRequestBuilder createTripRequest(String body) {
        return post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder listTripsRequest() {
        return get("/api/trips");
    }

    private MockHttpServletRequestBuilder getTripRequest(Long tripId) {
        return get("/api/trips/{tripId}", tripId);
    }

    private RequestPostProcessor login() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "여행자", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
