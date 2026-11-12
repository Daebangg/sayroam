package com.daebangg.sayroam.travelstyle.application.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record UpdateTravelStyleCommand(List<Answer> answers) {
    public UpdateTravelStyleCommand {
        answers = List.copyOf(answers);
    }

    public record Answer(String code, @Nullable Integer score) {
    }
}
