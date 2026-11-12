package com.daebangg.sayroam.travelstyle.application.dto;

import com.daebangg.sayroam.travelstyle.domain.TravelStyleItem;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import org.jspecify.annotations.Nullable;

public record TravelStyleItemResult(String code, @Nullable Integer score) {
    public static TravelStyleItemResult from(TravelStyleItem item, @Nullable UserTravelStyle answer) {
        return new TravelStyleItemResult(item.getCode(), answer == null ? null : answer.getScore());
    }
}
