package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.room.application.RoomService;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class RoomControllerTest {
    private static final Long USER_ID = 1L;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RoomService roomService;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void 로그인한_회원과_요청한_이름으로_방을_만들고_201과_방_ID를_응답한다() throws Exception {
        // given
        given(roomService.create(USER_ID, "제주도 여행")).willReturn(7L);

        // when & then
        mvc.perform(방_생성_요청("""
                        {"name": "제주도 여행"}
                        """).with(로그인()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(7));
    }

    @Test
    void 이름_없이_요청해도_방을_만든다() throws Exception {
        // given
        given(roomService.create(USER_ID, null)).willReturn(7L);

        // when & then
        mvc.perform(방_생성_요청("{}").with(로그인()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(7));
    }

    @Test
    void 방_이름이_너무_길면_400과_에러_코드를_응답한다() throws Exception {
        // given
        given(roomService.create(any(), any()))
                .willThrow(new BusinessException(RoomErrorCode.ROOM_NAME_TOO_LONG));

        // when & then
        mvc.perform(방_생성_요청("""
                        {"name": "너무 긴 이름"}
                        """).with(로그인()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ROOM_NAME_TOO_LONG"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_방을_만들지_않는다() throws Exception {
        // when & then
        mvc.perform(방_생성_요청("""
                        {"name": "제주도 여행"}
                        """))
                .andExpect(status().isUnauthorized());

        then(roomService).shouldHaveNoInteractions();
    }

    private MockHttpServletRequestBuilder 방_생성_요청(String body) {
        return post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private RequestPostProcessor 로그인() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "방장", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
