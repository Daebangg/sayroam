package com.daebangg.sayroam.destination.presentation.controller;

import com.daebangg.sayroam.destination.application.service.DestinationService;
import com.daebangg.sayroam.destination.presentation.api.DestinationApi;
import com.daebangg.sayroam.destination.presentation.dto.DestinationListResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DestinationController implements DestinationApi {
    private final DestinationService service;

    @Override
    @GetMapping(value = "/api/destinations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DestinationListResponse> list(@RequestParam(name = "q", required = false) @Nullable String query) {
        return ResponseEntity.ok(DestinationListResponse.from(service.findDestinations(query)));
    }
}
