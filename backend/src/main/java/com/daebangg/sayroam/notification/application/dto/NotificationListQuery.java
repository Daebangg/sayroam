package com.daebangg.sayroam.notification.application.dto;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;

public record NotificationListQuery(
        Long receiverId,
        Long beforeId,
        int size,
        boolean unreadOnly
) {
    public static final int MAX_PAGE_SIZE = 100;

    public NotificationListQuery {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }

        if (beforeId != null && beforeId <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
