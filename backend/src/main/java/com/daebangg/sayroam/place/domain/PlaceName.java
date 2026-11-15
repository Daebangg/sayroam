package com.daebangg.sayroam.place.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.validation.InputText;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 화면에 보이는 장소 이름. */
@Embeddable
public record PlaceName(
        @Column(name = "name", nullable = false, length = MAX_LENGTH)
        String value
) {
    public static final int MAX_LENGTH = 100;

    public PlaceName {
        if (value == null || value.isEmpty()) {
            throw new BusinessException(PlaceErrorCode.PLACE_NAME_REQUIRED);
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(PlaceErrorCode.PLACE_NAME_TOO_LONG);
        }
    }

    public static PlaceName from(String name) {
        return new PlaceName(InputText.trim(name));
    }

}
