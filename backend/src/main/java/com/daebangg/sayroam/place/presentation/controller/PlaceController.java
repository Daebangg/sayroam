package com.daebangg.sayroam.place.presentation.controller;

import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.service.PlaceService;
import com.daebangg.sayroam.place.presentation.api.PlaceApi;
import com.daebangg.sayroam.place.presentation.dto.PlaceSearchRequest;
import com.daebangg.sayroam.place.presentation.dto.PlaceSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceController implements PlaceApi {
    private final PlaceService placeService;

    @Override
    @GetMapping(value = "/api/places", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaceSearchResponse> search(@Valid @ModelAttribute PlaceSearchRequest request) {
        PlaceSearchResult result = placeService.search(request.toQuery());
        return ResponseEntity.ok(PlaceSearchResponse.from(result));
    }
}
