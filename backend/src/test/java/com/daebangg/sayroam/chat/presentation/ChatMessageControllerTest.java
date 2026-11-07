package com.daebangg.sayroam.chat.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.chat.application.ChatMessageService;
import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class ChatMessageControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 7L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 7, 10, 0);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ChatMessageService chatMessageService;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void afterId_이후_메시지를_200으로_응답한다() throws Exception {
        // given
        ChatMessageResult message = new ChatMessageResult(101L, ROOM_ID, USER_ID, "client-id", "안녕", CREATED_AT);
        given(chatMessageService.findAfter(ROOM_ID, USER_ID, 100L, 50)).willReturn(List.of(message));

        // when & then
        mvc.perform(findAfterRequest().param("afterId", "100").param("size", "50").with(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(101))
                .andExpect(jsonPath("$[0].roomId").value(ROOM_ID))
                .andExpect(jsonPath("$[0].senderId").value(USER_ID))
                .andExpect(jsonPath("$[0].clientMessageId").value("client-id"))
                .andExpect(jsonPath("$[0].content").value("안녕"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-07T10:00:00Z"));
    }

    @Test
    void afterId와_size를_생략하면_처음부터_100개까지_조회한다() throws Exception {
        // given
        ChatMessageResult message = new ChatMessageResult(1L, ROOM_ID, USER_ID, "client-id", "안녕", CREATED_AT);
        given(chatMessageService.findAfter(ROOM_ID, USER_ID, 0L, 100)).willReturn(List.of(message));

        // when & then
        mvc.perform(findAfterRequest().with(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101"})
    void 조회_개수가_범위를_벗어나면_400을_응답하고_조회하지_않는다(String size) throws Exception {
        // when & then
        mvc.perform(findAfterRequest().param("size", size).with(loggedIn()))
                .andExpect(status().isBadRequest());

        then(chatMessageService).shouldHaveNoInteractions();
    }

    @Test
    void 방_멤버가_아니면_403과_에러_코드를_응답한다() throws Exception {
        // given
        given(chatMessageService.findAfter(ROOM_ID, USER_ID, 0L, 100))
                .willThrow(new BusinessException(RoomErrorCode.ROOM_ACCESS_DENIED));

        // when & then
        mvc.perform(findAfterRequest().with(loggedIn()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACCESS_DENIED"));
    }

    @Test
    void 로그인하지_않으면_401을_응답하고_조회하지_않는다() throws Exception {
        // when & then
        mvc.perform(findAfterRequest())
                .andExpect(status().isUnauthorized());

        then(chatMessageService).shouldHaveNoInteractions();
    }

    private MockHttpServletRequestBuilder findAfterRequest() {
        return get("/api/rooms/{roomId}/messages", ROOM_ID);
    }

    private RequestPostProcessor loggedIn() {
        AuthenticatedAccount account = new AuthenticatedAccount(USER_ID, "user@example.com", "회원", null, Role.USER, 0L);
        return authentication(new UsernamePasswordAuthenticationToken(account, null, List.of()));
    }
}
