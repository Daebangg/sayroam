package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {
    @Test
    void 이름_없이_방을_만들_수_있다() {
        Room room = new Room(null);

        assertThatCode(() -> new Room(null))
                .doesNotThrowAnyException();
    }

    @Test
    void 방_이름이_20자를_넘으면_만들_수_없다() {
        String name = "A".repeat(21);

        assertThatThrownBy(() -> new Room(name))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 방_이름이_20자면_만들_수_있다() {
        String name = "A".repeat(20);

        assertThatCode(() -> new Room(name))
                .doesNotThrowAnyException();
    }

    @Test
    void 공백뿐인_이름은_이름이_없는_방으로_만든다() {
        Room room = new Room("     ");

        assertThat(room.hasName()).isFalse();
    }

    @Test
    void 방_이름의_앞뒤_공백은_제거하고_저장한다() {
        Room room = new Room("   제주 3박 4일  ");

        assertThat(room.getName()).isEqualTo("제주 3박 4일");
    }

    @Test
    void 이모지는_한_글자로_센다() {
        String name = "😀".repeat(20);

        assertThatCode(() -> new Room(name))
                .doesNotThrowAnyException();
    }

    @Test
    void 앞뒤_공백을_제거_길이로_검사한다() {
        String name = "            adsfdsfsdfdsfsf                 ";

        assertThatCode(() -> new Room(name))
                .doesNotThrowAnyException();

    }

}