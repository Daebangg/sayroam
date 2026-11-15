package com.daebangg.sayroam.place.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.place.application.dto.PlaceSearchItem;
import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.service.PlaceService;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import com.daebangg.sayroam.place.presentation.controller.PlaceController;
import com.daebangg.sayroam.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class PlaceControllerTest {
    private static final Long USER_ID = 1L;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlaceService placeService;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void 검색하면_200과_검색_결과와_다음_페이지_여부를_응답한다() throws Exception {
        // given
        given(placeService.search(new PlaceSearchQuery("해운대", PlaceCategory.LODGING, 2)))
                .willReturn(new PlaceSearchResult(List.of(new PlaceSearchItem(
                        "142785", "해운대 호텔", PlaceCategory.LODGING, null,
                        new BigDecimal("35.1587"), new BigDecimal("129.1604"))), true));

        // when & then
        mvc.perform(searchRequest()
                        .param("query", "해운대")
                        .param("category", "LODGING")
                        .param("page", "2")
                        .with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].sourceId").value("142785"))
                .andExpect(jsonPath("$.places[0].name").value("해운대 호텔"))
                .andExpect(jsonPath("$.places[0].category").value("LODGING"))
                .andExpect(jsonPath("$.places[0].address").isEmpty())
                .andExpect(jsonPath("$.places[0].latitude").value(35.1587))
                .andExpect(jsonPath("$.places[0].longitude").value(129.1604))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void 검색어_앞뒤_공백을_지우고_분류와_페이지가_없으면_모든_분류의_1페이지를_검색한다() throws Exception {
        // given
        given(placeService.search(new PlaceSearchQuery("해운대", null, 1)))
                .willReturn(new PlaceSearchResult(List.of(), false));

        // when & then
        mvc.perform(searchRequest().param("query", "  해운대 ").with(login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 검색어가_비어_있으면_400과_에러_코드를_응답하고_검색하지_않는다(String query) throws Exception {
        // when & then
        mvc.perform(searchRequest().param("query", query).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("query"));

        then(placeService).shouldHaveNoInteractions();
    }

    @Test
    void 검색어가_없으면_400을_응답하고_검색하지_않는다() throws Exception {
        // when & then
        mvc.perform(searchRequest().with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("query"));

        then(placeService).shouldHaveNoInteractions();
    }

    @Test
    void 검색어가_100자를_넘으면_400을_응답하고_검색하지_않는다() throws Exception {
        // when & then
        mvc.perform(searchRequest().param("query", "가".repeat(101)).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("query"));

        then(placeService).shouldHaveNoInteractions();
    }

    @Test
    void 없는_분류면_400을_응답하고_검색하지_않는다() throws Exception {
        // when & then
        mvc.perform(searchRequest().param("query", "해운대").param("category", "PARK").with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("category"));

        then(placeService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "51"})
    void 페이지가_1에서_50_사이가_아니면_400을_응답하고_검색하지_않는다(String page) throws Exception {
        // when & then
        mvc.perform(searchRequest().param("query", "해운대").param("page", page).with(login()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        then(placeService).shouldHaveNoInteractions();
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_검색하지_않는다() throws Exception {
        // when & then
        mvc.perform(searchRequest().param("query", "해운대"))
                .andExpect(status().isUnauthorized());

        then(placeService).shouldHaveNoInteractions();
    }

    @Test
    void 외부_검색이_실패하면_502와_에러_코드를_응답한다() throws Exception {
        // given
        given(placeService.search(any(PlaceSearchQuery.class)))
                .willThrow(new BusinessException(PlaceErrorCode.PLACE_SEARCH_FAILED));

        // when & then
        mvc.perform(searchRequest().param("query", "해운대").with(login()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PLACE_SEARCH_FAILED"));
    }

    @Test
    void 검색이_꺼져_있으면_503과_에러_코드를_응답한다() throws Exception {
        // given
        given(placeService.search(any(PlaceSearchQuery.class)))
                .willThrow(new BusinessException(PlaceErrorCode.PLACE_SEARCH_DISABLED));

        // when & then
        mvc.perform(searchRequest().param("query", "해운대").with(login()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PLACE_SEARCH_DISABLED"));
    }

    private MockHttpServletRequestBuilder searchRequest() {
        return get("/api/places");
    }

    private RequestPostProcessor login() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "여행자", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
