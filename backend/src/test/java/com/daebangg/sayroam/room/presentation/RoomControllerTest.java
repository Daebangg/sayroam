package com.daebangg.sayroam.room.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.RoomMemberStatus;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.presentation.controller.RoomController;
import com.daebangg.sayroam.user.domain.Role;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class RoomControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 7L;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RoomService roomService;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void 나가기에_성공하면_204를_응답한다() throws Exception {
        // when & then
        mvc.perform(leaveRequest().with(로그인()))
                .andExpect(status().isNoContent());

        then(roomService).should().leave(ROOM_ID, USER_ID);
    }

    @Test
    void 참여_중이_아닌_방을_나가면_403과_에러_코드를_응답한다() throws Exception {
        // given
        willThrow(new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED))
                .given(roomService).leave(ROOM_ID, USER_ID);

        // when & then
        mvc.perform(leaveRequest().with(로그인()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACCESS_DENIED"));
    }

    @Test
    void 로그인하지_않으면_나가기에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(leaveRequest())
                .andExpect(status().isUnauthorized());

        then(roomService).shouldHaveNoInteractions();
    }

    @Test
    void 멤버_목록을_조회하면_200과_멤버를_응답한다() throws Exception {
        // given
        given(roomService.findMembers(ROOM_ID, USER_ID)).willReturn(List.of(
                new RoomMemberProfileResult(USER_ID, "민수", "https://example.com/minsu.png", RoomMemberStatus.JOINED),
                new RoomMemberProfileResult(3L, "지영", null, RoomMemberStatus.LEFT)));

        // when & then
        mvc.perform(findMembersRequest().with(로그인()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].userId").value(1))
                .andExpect(jsonPath("$.members[0].nickname").value("민수"))
                .andExpect(jsonPath("$.members[0].profileImageUrl").value("https://example.com/minsu.png"))
                .andExpect(jsonPath("$.members[0].status").value("JOINED"))
                .andExpect(jsonPath("$.members[1].profileImageUrl").isEmpty())
                .andExpect(jsonPath("$.members[1].status").value("LEFT"));
    }

    @Test
    void 방_멤버가_아니면_멤버_목록_조회에_403과_에러_코드를_응답한다() throws Exception {
        // given
        given(roomService.findMembers(ROOM_ID, USER_ID))
                .willThrow(new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED));

        // when & then
        mvc.perform(findMembersRequest().with(로그인()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACCESS_DENIED"));
    }

    @Test
    void 로그인하지_않으면_멤버_목록_조회에_401을_응답한다() throws Exception {
        // when & then
        mvc.perform(findMembersRequest())
                .andExpect(status().isUnauthorized());

        then(roomService).shouldHaveNoInteractions();
    }

    private MockHttpServletRequestBuilder leaveRequest() {
        return delete("/api/rooms/{roomId}/members/me", ROOM_ID);
    }

    private MockHttpServletRequestBuilder findMembersRequest() {
        return get("/api/rooms/{roomId}/members", ROOM_ID);
    }

    private RequestPostProcessor 로그인() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "방장", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
