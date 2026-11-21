package com.daebangg.sayroam.trip.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.presentation.api.TripApi;
import com.daebangg.sayroam.trip.presentation.dto.AddTripCustomPlaceRequest;
import com.daebangg.sayroam.trip.presentation.dto.AddTripPlaceRequest;
import com.daebangg.sayroam.trip.presentation.dto.AddTripPlaceResponse;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRoomResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripPlaceListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripSummaryResponse;
import com.daebangg.sayroam.trip.presentation.dto.UpdateTripRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TripController implements TripApi {
    private final TripService tripService;

    @Override
    @PostMapping(value = "/api/trips", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateTripResponse> create(@AuthenticationPrincipal AuthenticatedAccount account,
                                                     @Valid @RequestBody CreateTripRequest request) {
        Long tripId = tripService.create(account.userId(), request.startDate(), request.endDate(), request.title());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTripResponse(tripId));
    }

    @Override
    @PatchMapping(value = "/api/trips/{tripId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripSummaryResponse> update(@AuthenticationPrincipal AuthenticatedAccount account,
                                                      @PathVariable Long tripId,
                                                      @RequestBody UpdateTripRequest request) {
        TripSummaryResult result = tripService.rename(account.userId(), tripId, request.title());
        return ResponseEntity.ok(TripSummaryResponse.from(result));
    }

    @Override
    @PostMapping(value = "/api/trips/{tripId}/room", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateTripRoomResponse> createRoom(@AuthenticationPrincipal AuthenticatedAccount account,
                                                             @PathVariable Long tripId,
                                                             @RequestBody CreateTripRoomRequest request) {
        Long roomId = tripService.createRoom(account.userId(), tripId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTripRoomResponse(roomId));
    }

    @Override
    @PostMapping(value = "/api/trips/{tripId}/places", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddTripPlaceResponse> addPlace(@AuthenticationPrincipal AuthenticatedAccount account,
                                                         @PathVariable Long tripId,
                                                         @Valid @RequestBody AddTripPlaceRequest request) {
        Long tripPlaceId = tripService.addPlace(account.userId(), tripId, request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AddTripPlaceResponse(tripPlaceId));
    }

    @Override
    @PostMapping(value = "/api/trips/{tripId}/custom-places", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddTripPlaceResponse> addCustomPlace(@AuthenticationPrincipal AuthenticatedAccount account,
                                                               @PathVariable Long tripId,
                                                               @Valid @RequestBody AddTripCustomPlaceRequest request) {
        Long tripPlaceId = tripService.addCustomPlace(account.userId(), tripId, request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AddTripPlaceResponse(tripPlaceId));
    }

    @Override
    @GetMapping(value = "/api/trips", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripListResponse> findMine(@AuthenticationPrincipal AuthenticatedAccount account) {
        List<TripSummaryResult> results = tripService.findMine(account.userId());
        return ResponseEntity.ok(TripListResponse.from(results));
    }

    @Override
    @GetMapping(value = "/api/trips/{tripId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripSummaryResponse> find(@AuthenticationPrincipal AuthenticatedAccount account,
                                                    @PathVariable Long tripId) {
        TripSummaryResult result = tripService.find(account.userId(), tripId);
        return ResponseEntity.ok(TripSummaryResponse.from(result));
    }

    @Override
    @GetMapping(value = "/api/trips/{tripId}/places", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TripPlaceListResponse> findPlaces(@AuthenticationPrincipal AuthenticatedAccount account,
                                                            @PathVariable Long tripId) {
        List<TripPlaceResult> results = tripService.findPlaces(account.userId(), tripId);
        return ResponseEntity.ok(TripPlaceListResponse.from(results));
    }
}
