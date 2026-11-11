package com.daebangg.sayroam.trip.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.presentation.api.TripApi;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripListResponse;
import com.daebangg.sayroam.trip.presentation.dto.TripSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
        Long tripId = tripService.create(account.userId(), request.startDate(), request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTripResponse(tripId));
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
}
