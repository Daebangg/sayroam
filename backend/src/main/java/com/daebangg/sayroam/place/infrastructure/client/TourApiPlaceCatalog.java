package com.daebangg.sayroam.place.infrastructure.client;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.dto.PlaceSearchItem;
import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국관광공사 TourAPI 키워드 검색 결과를 저장하지 않고 검색 결과로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class TourApiPlaceCatalog implements PlaceCatalog {

    private static final int PAGE_SIZE = 20;
    // TourAPI가 정상응답일 때 "0000"을 준다.
    private static final String SUCCESS_CODE = "0000";

    private final RestClient tourApi;
    private final TourApiProperties properties;

    @Override
    public PlaceSearchResult search(PlaceSearchQuery query) {
        if (!properties.enabled()) {
            throw new BusinessException(PlaceErrorCode.PLACE_SEARCH_DISABLED);
        }
        JsonNode body = successBody(searchKeyword(query));
        return new PlaceSearchResult(places(body.path("items").path("item")), hasNext(body));
    }

    private JsonNode searchKeyword(PlaceSearchQuery query) {
        try {
            return tourApi.get()
                    .uri(uri -> searchKeywordUri(uri, query))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // 응답 본문과 요청 URL에 서비스 키가 있을 수 있어 원인 메시지를 밖으로 내보내지 않는다.
            throw searchFailed();
        }
    }

    // 값은 URI 변수로 넘겨 '+', '/' 같은 서비스 키 문자도 한 번만 엄격하게 인코딩되게 한다.
    private URI searchKeywordUri(UriBuilder uri, PlaceSearchQuery query) {
        uri.path("/searchKeyword2")
                .queryParam("serviceKey", "{serviceKey}")
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "sayroam")
                .queryParam("_type", "json")
                .queryParam("keyword", "{keyword}")
                .queryParam("pageNo", query.page())
                .queryParam("numOfRows", PAGE_SIZE);
        if (query.category() != null) {
            uri.queryParam("contentTypeId", TourContentType.codeOf(query.category()));
        }
        return uri.build(Map.of("serviceKey", properties.serviceKey(), "keyword", query.keyword()));
    }

    private JsonNode successBody(JsonNode response) {
        if (response == null) {
            throw searchFailed();
        }
        JsonNode result = response.path("response");
        if (!SUCCESS_CODE.equals(result.path("header").path("resultCode").asString(""))) {
            throw searchFailed();
        }
        return result.path("body");
    }

    // 결과가 없으면 items가 빈 문자열이다. 실제 응답은 한 건도 배열로 오지만,
    // XML을 JSON으로 바꾸는 공공데이터 API에서 단일 항목이 객체로 오는 경우를 함께 처리한다.
    private List<PlaceSearchItem> places(JsonNode items) {
        List<JsonNode> nodes = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(nodes::add);
        } else if (items.isObject()) {
            nodes.add(items);
        }
        return nodes.stream()
                .map(this::place)
                .flatMap(Optional::stream)
                .toList();
    }

    // 서비스 분류가 없거나 지도에 표시할 좌표가 없는 항목은 담을 수 없어 뺀다.
    private Optional<PlaceSearchItem> place(JsonNode item) {
        Optional<PlaceCategory> category = TourContentType.categoryOf(item.path("contenttypeid").asString(""));
        Optional<BigDecimal> latitude = decimal(item.path("mapy"));
        Optional<BigDecimal> longitude = decimal(item.path("mapx"));
        if (category.isEmpty() || latitude.isEmpty() || longitude.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlaceSearchItem(
                item.path("contentid").asString(""),
                item.path("title").asString(""),
                category.get(),
                text(item.path("addr1")),
                latitude.get(),
                longitude.get()));
    }

    private Optional<BigDecimal> decimal(JsonNode node) {
        String value = node.asString("").trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String text(JsonNode node) {
        String value = node.asString("").trim();
        return value.isEmpty() ? null : value;
    }

    private boolean hasNext(JsonNode body) {
        // 큰 페이지 번호에서 곱셈이 int 범위를 넘지 않도록 long으로 계산한다.
        long shown = (long) body.path("pageNo").asInt() * body.path("numOfRows").asInt();
        return shown < body.path("totalCount").asLong();
    }

    private BusinessException searchFailed() {
        return new BusinessException(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

}
