package com.daebangg.sayroam.trip.application.port.out;

import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;

import java.util.List;
import java.util.Optional;

public interface TripQueryRepository {
    List<TripSummaryResult> findAllReadable(Long userId);

    Optional<TripSummaryResult> findReadable(Long userId, Long tripId);
}
