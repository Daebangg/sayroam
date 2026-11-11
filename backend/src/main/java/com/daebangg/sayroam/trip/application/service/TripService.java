package com.daebangg.sayroam.trip.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;

    @Transactional
    public Long create(Long userId, LocalDate startDate, LocalDate endDate) {
        TravelPeriod period = new TravelPeriod(startDate, endDate);
        Trip trip = tripRepository.save(new Trip(userId, period));
        return trip.getId();
    }

}
