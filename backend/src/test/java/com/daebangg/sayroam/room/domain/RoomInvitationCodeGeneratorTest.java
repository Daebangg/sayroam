package com.daebangg.sayroam.room.domain;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class RoomInvitationCodeGeneratorTest {

    @Test
    void 생성한_코드는_50자를_넘지_않는다() {
        // when
        String code = RoomInvitationCodeGenerator.generate();

        // then
        assertThat(code.length()).isLessThanOrEqualTo(50);
    }

    @Test
    void 생성한_코드는_URL에_안전한_문자만_사용한다() {
        // when
        String code = RoomInvitationCodeGenerator.generate();

        // then
        assertThat(code).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void 생성한_코드는_32바이트_난수를_담는다() {
        // when
        String code = RoomInvitationCodeGenerator.generate();

        // then
        assertThat(Base64.getUrlDecoder().decode(code)).hasSize(32);
    }

    @Test
    void 호출할_때마다_다른_코드를_생성한다() {
        // when
        String first = RoomInvitationCodeGenerator.generate();
        String second = RoomInvitationCodeGenerator.generate();

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
