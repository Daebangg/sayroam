package com.daebangg.sayroam.trip.application.service;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.service.PlaceService;
import com.daebangg.sayroam.place.domain.Coordinate;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.trip.application.dto.AddTripCustomPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.AddTripPlaceCommand;
import com.daebangg.sayroam.trip.application.dto.CreateTripCommand;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.dto.TripTitleResult;
import com.daebangg.sayroam.trip.application.port.out.TripQueryRepository;
import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripPlace;
import com.daebangg.sayroam.trip.domain.TripPlaceRepository;
import com.daebangg.sayroam.trip.domain.TripRepository;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final TripQueryRepository tripQueryRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final PlaceService placeService;
    private final RoomService roomService;
    private final DestinationRepository destinationRepository;

    @Transactional
    public Long create(Long userId, CreateTripCommand command) {
        TravelPeriod period = new TravelPeriod(command.startDate(), command.endDate());
        Trip trip = new Trip(userId, command.destinationId(), period, command.title());
        requireActiveDestination(command.destinationId());
        return tripRepository.save(trip).getId();
    }

    private void requireActiveDestination(Long destinationId) {
        if (!destinationRepository.existsByIdAndActiveTrue(destinationId)) {
            throw new BusinessException(TripErrorCode.TRIP_DESTINATION_UNAVAILABLE);
        }
    }

    @Transactional
    public TripTitleResult rename(Long userId, Long tripId, String title) {
        // 잠금 없이 읽으면 동시에 커밋된 방 연결을 이전 값으로 덮어쓴다
        Trip trip = tripRepository.findAccessibleForUpdate(userId, tripId)
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));

        if (title != null) {
            trip.rename(title);
        }
        return TripTitleResult.from(trip);
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

    @Transactional
    public Long addPlace(Long userId, Long tripId, AddTripPlaceCommand command) {
        Trip trip = findPlaceAddableTrip(userId, tripId, command.day());
        Long placeId = placeService.register(command.sourceId());
        TripPlace tripPlace = new TripPlace(trip, placeId, userId, command.day(), command.memo());
        return tripPlaceRepository.save(tripPlace).getId();
    }

    // 장소를 저장하기 전에 추가 권한과 일차를 먼저 확인해 거부될 요청이 외부 조회나 저장을 하지 않게 한다.
    private Trip findPlaceAddableTrip(Long userId, Long tripId, Integer day) {
        Trip trip = tripRepository.findAccessible(userId, tripId)
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));
        trip.ensurePlaceAddable(day);
        return trip;
    }

    @Transactional
    public Long addCustomPlace(Long userId, Long tripId, AddTripCustomPlaceCommand command) {
        Trip trip = findPlaceAddableTrip(userId, tripId, command.day());
        Long placeId = placeService.registerCustom(command.name(),
                new Coordinate(command.latitude(), command.longitude()));
        TripPlace tripPlace = new TripPlace(trip, placeId, userId, command.day(), command.memo());
        return tripPlaceRepository.save(tripPlace).getId();
    }

    public List<TripSummaryResult> findMine(Long userId) {
        return tripQueryRepository.findAllReadable(userId);
    }

    public TripSummaryResult find(Long userId, Long tripId) {
        return tripQueryRepository.findReadable(userId, tripId)
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));
    }

    public List<TripPlaceResult> findPlaces(Long userId, Long tripId) {
        tripQueryRepository.findReadable(userId, tripId)
                .orElseThrow(() -> new BusinessException(TripErrorCode.TRIP_NOT_FOUND));
        return tripQueryRepository.findPlaces(tripId);
    }
}
