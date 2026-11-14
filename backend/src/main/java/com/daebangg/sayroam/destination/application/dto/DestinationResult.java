package com.daebangg.sayroam.destination.application.dto;

import com.daebangg.sayroam.destination.domain.Destination;

public record DestinationResult(long id, String name) {
    public static DestinationResult from(Destination destination) {
        return new DestinationResult(destination.getId(), destination.getName());
    }
}
