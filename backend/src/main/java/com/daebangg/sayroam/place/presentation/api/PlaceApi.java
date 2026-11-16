package com.daebangg.sayroam.place.presentation.api;

import com.daebangg.sayroam.place.presentation.dto.PlaceSearchRequest;
import com.daebangg.sayroam.place.presentation.dto.PlaceSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "Place")
public interface PlaceApi {
    @Operation(operationId = "searchPlaces", summary = "장소 키워드 검색",
            description = "로그인한 회원이 여행에 담을 장소를 키워드로 검색합니다. 한국관광공사 TourAPI를 요청마다 호출하며 결과는 저장하지 않습니다. "
                    + "한 페이지는 최대 20개이고, 축제·여행코스와 좌표가 없는 장소는 제외합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 결과. 결과가 없으면 빈 배열"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/PlaceSearchBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/PlaceSearchFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PlaceSearchUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<PlaceSearchResponse> search(@ParameterObject PlaceSearchRequest request);
}
