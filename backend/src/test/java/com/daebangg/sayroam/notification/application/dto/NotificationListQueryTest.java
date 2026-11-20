package com.daebangg.sayroam.notification.application.dto;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationListQueryTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void 허용_범위_밖의_조회_개수는_객체_생성_시_거부한다(int size) {
        assertThatThrownBy(() -> new NotificationListQuery(1L, null, size, false))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, 0L})
    void 양수가_아닌_기준_ID는_객체_생성_시_거부한다(long beforeId) {
        assertThatThrownBy(() -> new NotificationListQuery(1L, beforeId, 20, false))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }
}
