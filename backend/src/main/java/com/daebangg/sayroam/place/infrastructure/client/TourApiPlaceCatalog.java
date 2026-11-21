package com.daebangg.sayroam.place.infrastructure.client;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.dto.PlaceSearchItem;
import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import com.daebangg.sayroam.place.domain.Coordinate;
import com.daebangg.sayroam.place.domain.Place;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceName;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Function;

/**
 * 한국관광공사 TourAPI로 장소를 검색하고, 여행에 담을 장소의 상세 정보를 조회한다.
 */
@Slf4j
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
        ensureEnabled();
        JsonNode body = successBody(get(uri -> searchKeywordUri(uri, query)));
        List<PlaceSearchItem> places = itemNodes(body).stream()
                .map(this::place)
                .flatMap(Optional::stream)
                .toList();
        return new PlaceSearchResult(places, hasNext(body));
    }

    private URI searchKeywordUri(UriBuilder uri, PlaceSearchQuery query) {
        withCommonParams(uri.path("/searchKeyword2"))
                .queryParam("keyword", "{keyword}")
                .queryParam("pageNo", query.page())
                .queryParam("numOfRows", PAGE_SIZE);
        if (query.category() != null) {
            uri.queryParam("contentTypeId", TourContentType.codeOf(query.category()));
        }
        return uri.build(Map.of("serviceKey", properties.serviceKey(), "keyword", query.keyword()));
    }

    // 페이지 정보가 없거나 숫자가 아니면 0으로 보지 않고 실패로 처리한다. 결과가 없을 때 numOfRows는 0이다.
    private boolean hasNext(JsonNode body) {
        // 큰 페이지 번호에서 곱셈이 int 범위를 넘지 않도록 long으로 계산한다.
        long shown = number(body.path("pageNo")) * number(body.path("numOfRows"));
        return shown < number(body.path("totalCount"));
    }

    private long number(JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw searchFailed();
        }
        return node.asLong();
    }

    // 없는 장소 ID면 검색 결과가 없을 때처럼 items가 빈 문자열로 온다고 본다.
    @Override
    public Optional<Place> find(String sourceId) {
        ensureEnabled();
        JsonNode body = successBody(get(uri -> detailCommonUri(uri, sourceId)));
        return itemNodes(body).stream()
                .findFirst()
                .flatMap(item -> place(item).flatMap(found -> toPlace(found, item)));
    }

    private URI detailCommonUri(UriBuilder uri, String sourceId) {
        withCommonParams(uri.path("/detailCommon2"))
                .queryParam("contentId", "{contentId}");
        return uri.build(Map.of("serviceKey", properties.serviceKey(), "contentId", sourceId));
    }

    // 원본 분류 코드(contenttypeid)를 함께 남겨 서비스 분류를 바꿀 때 다시 분류할 수 있게 한다.
    // 출처 데이터가 장소 규칙(원본 ID·주소 길이 등)을 어기면 회원 입력 오류가 아니라 담을 수 없는 장소로 본다.
    private Optional<Place> toPlace(PlaceSearchItem found, JsonNode item) {
        try {
            return Optional.of(Place.tour(
                    found.sourceId(),
                    new PlaceName(found.name()),
                    found.category(),
                    item.path("contenttypeid").asString(""),
                    found.address(),
                    new Coordinate(found.latitude(), found.longitude())));
        } catch (BusinessException e) {
            log.warn("TourAPI place rejected: contentId={}", found.sourceId());
            return Optional.empty();
        }
    }

    private void ensureEnabled() {
        if (!properties.enabled()) {
            throw new BusinessException(PlaceErrorCode.PLACE_SEARCH_DISABLED);
        }
    }

    private JsonNode get(Function<UriBuilder, URI> uri) {
        try {
            return tourApi.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // 응답 본문과 요청 URL에 서비스 키가 있을 수 있어 원인 메시지를 밖으로 내보내지 않는다.
            throw searchFailed();
        }
    }

    // 값은 URI 변수로 넘겨 '+', '/' 같은 서비스 키 문자도 한 번만 엄격하게 인코딩되게 한다.
    private UriBuilder withCommonParams(UriBuilder uri) {
        return uri.queryParam("serviceKey", "{serviceKey}")
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "sayroam")
                .queryParam("_type", "json");
    }

    private JsonNode successBody(JsonNode response) {
        if (response == null) {
            throw searchFailed();
        }
        JsonNode result = response.path("response");
        if (!SUCCESS_CODE.equals(result.path("header").path("resultCode").asString(""))) {
            throw searchFailed();
        }
        // 성공 코드여도 본문이 없으면 장애를 "결과 없음"으로 숨기지 않고 실패로 본다.
        JsonNode body = result.path("body");
        if (!body.isObject()) {
            throw searchFailed();
        }
        return body;
    }

    // 결과가 없으면 items가 빈 문자열이다. 실제 응답은 한 건도 배열로 오지만,
    // XML을 JSON으로 바꾸는 공공데이터 API에서 단일 항목이 객체로 오는 경우를 함께 처리한다.
    private List<JsonNode> itemNodes(JsonNode body) {
        JsonNode items = body.path("items").path("item");
        List<JsonNode> nodes = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(nodes::add);
        } else if (items.isObject()) {
            nodes.add(items);
        }
        return nodes;
    }

    // 사용하는 값만 검증한다. 장소 ID·이름·서비스 분류·좌표 중 하나라도 없거나,
    // 이름·좌표가 장소 규칙을 어기면 담을 수 없어 뺀다. 검색에 보인 장소는 추가도 되도록 같은 규칙을 쓴다.
    private Optional<PlaceSearchItem> place(JsonNode item) {
        String sourceId = text(item.path("contentid"));
        String name = text(item.path("title"));
        Optional<PlaceCategory> category = TourContentType.categoryOf(item.path("contenttypeid").asString(""));
        Optional<BigDecimal> latitude = decimal(item.path("mapy"));
        Optional<BigDecimal> longitude = decimal(item.path("mapx"));
        if (sourceId == null || name == null || category.isEmpty() || latitude.isEmpty() || longitude.isEmpty()
                || !followsPlaceRules(sourceId, name, latitude.get(), longitude.get())) {
            return Optional.empty();
        }
        return Optional.of(new PlaceSearchItem(
                sourceId,
                name,
                category.get(),
                text(item.path("addr1")),
                latitude.get(),
                longitude.get()));
    }

    // 규칙은 값 객체가 가지고, 여기서는 출처 데이터가 그 규칙을 지키는지만 확인한다.
    private boolean followsPlaceRules(String sourceId, String name, BigDecimal latitude, BigDecimal longitude) {
        try {
            new PlaceName(name);
            new Coordinate(latitude, longitude);
            return true;
        } catch (BusinessException e) {
            log.warn("TourAPI place rejected: contentId={}", sourceId);
            return false;
        }
    }

    private String text(JsonNode node) {
        String value = node.asString("").trim();
        return value.isEmpty() ? null : value;
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

    private BusinessException searchFailed() {
        return new BusinessException(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

}
