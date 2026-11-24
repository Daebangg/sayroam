package com.daebangg.sayroam.trip.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TripInvitationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 12, 0);

    @Test
    void 초대를_만들면_초대_코드가_발급된다() {
        // when
        TripInvitation invitation = TripInvitation.issue(NOW);

        // then
        assertThat(invitation.getCode()).isNotBlank();
    }

    @Test
    void 초대_코드는_50자를_넘지_않는다() {
        // when
        String code = TripInvitation.issue(NOW).getCode();

        // then
        assertThat(code.length()).isLessThanOrEqualTo(50);
    }

    @Test
    void 초대_코드는_URL에_안전한_문자만_사용한다() {
        // when
        String code = TripInvitation.issue(NOW).getCode();

        // then
        assertThat(code).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void 초대_코드는_32바이트_난수를_담는다() {
        // when
        String code = TripInvitation.issue(NOW).getCode();

        // then
        assertThat(Base64.getUrlDecoder().decode(code)).hasSize(32);
    }

    @Test
    void 초대마다_다른_코드가_발급된다() {
        // when
        String first = TripInvitation.issue(NOW).getCode();
        String second = TripInvitation.issue(NOW).getCode();

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
