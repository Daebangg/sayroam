package com.daebangg.sayroam.trip.presentation;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.trip.application.TripService;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripRequest;
import com.daebangg.sayroam.trip.presentation.dto.CreateTripResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TripController implements TripApi {
    private final TripService tripService;

    @Override
    @PostMapping("/api/trips")
    public ResponseEntity<CreateTripResponse> create(@AuthenticationPrincipal AuthenticatedAccount account,
                                                     @Valid @RequestBody CreateTripRequest request) {
        Long tripId = tripService.create(account.userId(), request.startDate(), request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTripResponse(tripId));
    }

}
