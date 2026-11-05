package com.daebangg.sayroam.chat.domain;

import com.daebangg.sayroam.chat.domain.error.ChatErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {
    private static final Long ROOM_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final String CLIENT_MESSAGE_ID = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10";

    @Test
    void 본문이_공백뿐이면_생성을_거부한다() {
        // given
        String content = "    \n     ";

        // when & then
        assertThatThrownBy(() -> ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_CONTENT_BLANK));
    }

    @Test
    void 본문이_null이면_생성을_거부한다() {
        assertThatThrownBy(() -> ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_CONTENT_BLANK));
    }

    @Test
    void 본문이_500자를_넘으면_생성을_거부한다() {
        // given
        String content = "A".repeat(501);

        // when & then
        assertThatThrownBy(() -> ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_CONTENT_TOO_LONG));
    }

    @Test
    void 본문이_500자면_생성한다() {
        // given
        String content = "A".repeat(500);

        assertThatCode(() -> ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content))
                .doesNotThrowAnyException();
    }

    @Test
    void 이모지는_한_글자로_센다() {
        // given
        String content = "😀".repeat(500);

        assertThatCode(() -> ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content))
                .doesNotThrowAnyException();
    }

    @Test
    void 본문의_앞뒤_공백과_줄바꿈을_유지한다() {
        String content = " dada\n  ddww\n  \t";

        ChatMessage message = ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content);

        assertThat(message.getContent()).isEqualTo(content);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "hello", "1-1-1-1-1"})
    void 클라이언트_메시지_ID가_UUID가_아니면_생성을_거부한다(String clientMessageId) {
        String content = "A";

        assertThatThrownBy(() -> ChatMessage.text(ROOM_ID, SENDER_ID, clientMessageId, content))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ChatErrorCode.CHAT_CLIENT_MESSAGE_ID_INVALID));
    }

    @Test
    void 텍스트_메시지를_생성한다() {
        // given
        String content = "안녕";

        // when
        ChatMessage message = ChatMessage.text(ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID, content);

        // then
        assertThat(message.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
        assertThat(message.getType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getClientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);

    }

}
