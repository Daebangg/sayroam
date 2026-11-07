package com.daebangg.sayroam.chat.presentation.stomp;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatPrincipalTest {
    private static final long AUTH_VERSION = 3L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    void 사용자_ID를_이름으로_돌려준다() {
        // given
        ChatPrincipal principal = new ChatPrincipal(7L, AUTH_VERSION, EXPIRES_AT);

        assertThat(principal.getName()).isEqualTo("7");
    }

    @Test
    void 만료_전이면_통과한다() {
        // given
        ChatPrincipal principal = new ChatPrincipal(7L, AUTH_VERSION, EXPIRES_AT);

        assertThatCode(() -> principal.ensureNotExpired(EXPIRES_AT.minusSeconds(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void 만료_시각이_지나면_거부한다() {
        ChatPrincipal principal = new ChatPrincipal(7L, AUTH_VERSION, EXPIRES_AT);

        assertThatThrownBy(() -> principal.ensureNotExpired(EXPIRES_AT.plusSeconds(1)))
                .isInstanceOf(StompAuthenticationException.class);
    }

    @Test
    void 만료_시각과_같으면_거부한다() {
        // given
        ChatPrincipal principal = new ChatPrincipal(7L, AUTH_VERSION, EXPIRES_AT);

        assertThatThrownBy(() -> principal.ensureNotExpired(EXPIRES_AT))
                .isInstanceOf(StompAuthenticationException.class);
    }

}
