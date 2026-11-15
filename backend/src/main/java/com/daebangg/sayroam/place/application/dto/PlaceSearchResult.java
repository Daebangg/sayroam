package com.daebangg.sayroam.place.application.dto;

import java.util.List;

public record PlaceSearchResult(List<PlaceSearchItem> places, boolean hasNext) {
}
