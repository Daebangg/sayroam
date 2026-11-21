package com.daebangg.sayroam.trip.application.port.out;

import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;

import java.util.List;
import java.util.Optional;

public interface TripQueryRepository {
    List<TripSummaryResult> findAllReadable(Long userId);

    Optional<TripSummaryResult> findReadable(Long userId, Long tripId);

    // 권한은 확인하지 않는다. 호출 전에 findReadable로 볼 수 있는 여행인지 확인한다.
    List<TripPlaceResult> findPlaces(Long tripId);
}
