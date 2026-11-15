package com.daebangg.sayroam.place.presentation.dto;

import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceSearchRequest(
        @Schema(description = "검색어. 앞뒤 공백은 제외하고 검색", example = "해운대", maxLength = MAX_QUERY_LENGTH)
        @NotBlank
        @Size(max = MAX_QUERY_LENGTH)
        String query,
        @Schema(description = "분류. 생략하면 모든 분류를 검색", example = "LODGING", types = {"string", "null"})
        PlaceCategory category,
        @Schema(description = "페이지 번호. 1부터 시작하며 생략하면 1", example = "1",
                minimum = "1", maximum = "" + MAX_PAGE, defaultValue = "1", types = {"integer", "null"})
        @Min(1)
        @Max(MAX_PAGE)
        Integer page) {

    public static final int MAX_QUERY_LENGTH = 100;
    public static final int MAX_PAGE = 50;
    private static final int FIRST_PAGE = 1;

    public PlaceSearchQuery toQuery() {
        return new PlaceSearchQuery(query.strip(), category, page == null ? FIRST_PAGE : page);
    }
}
