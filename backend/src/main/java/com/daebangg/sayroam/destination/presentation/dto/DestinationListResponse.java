package com.daebangg.sayroam.destination.presentation.dto;

import com.daebangg.sayroam.destination.application.dto.DestinationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"destinations"})
public record DestinationListResponse(List<DestinationSummary> destinations) {

    public static DestinationListResponse from(List<DestinationResult> results) {
        return new DestinationListResponse(results.stream()
                .map(result -> new DestinationSummary(result.id(), result.name()))
                .toList());
    }

    @Schema(requiredProperties = {"id", "name"})
    public record DestinationSummary(long id, String name) {
    }
}
