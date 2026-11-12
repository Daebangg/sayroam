package com.daebangg.sayroam.trip.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripRepository;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final RoomService roomService;

    @Transactional
    public Long create(Long userId, LocalDate startDate, LocalDate endDate) {
        TravelPeriod period = new TravelPeriod(startDate, endDate);
        Trip trip = tripRepository.save(new Trip(userId, period));
        return trip.getId();
    }

    @Transactional
    public Long createRoom(Long userId, Long tripId, String roomName) {
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .filter(t -> t.isCreatedBy(userId))
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));
        trip.ensureRoomLinkable();
        Long roomId = roomService.create(userId, roomName);
        trip.linkRoom(roomId);
        return roomId;
    }

    public List<TripSummaryResult> findMine(Long userId) {
        return tripRepository.findAllByCreatedByIdOrderByIdDesc(userId).stream()
                .map(TripSummaryResult::from)
                .toList();
    }

    public TripSummaryResult find(Long userId, Long tripId) {
        return tripRepository.findById(tripId)
                .filter(trip -> trip.isCreatedBy(userId))
                .map(TripSummaryResult::from)
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));
    }
}
