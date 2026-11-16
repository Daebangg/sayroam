package com.daebangg.sayroam.place.infrastructure.client;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.dto.PlaceSearchItem;
import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiPlaceCatalogTest {

    private static final String BASE_URL = "https://tour.test/B551011/KorService2";
    private static final String SERVICE_KEY = "test-service-key";

    private MockRestServiceServer server;
    private TourApiPlaceCatalog catalog;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        catalog = new TourApiPlaceCatalog(builder.build(), enabledProperties());
    }

    @Test
    void 키워드와_분류_코드와_페이지를_쿼리로_보낸다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andExpect(queryParam("serviceKey", SERVICE_KEY))
                .andExpect(queryParam("keyword", encoded("해운대")))
                .andExpect(queryParam("contentTypeId", "32"))
                .andExpect(queryParam("pageNo", "2"))
                .andExpect(queryParam("numOfRows", "20"))
                .andExpect(queryParam("_type", "json"))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        // when
        catalog.search(new PlaceSearchQuery("해운대", PlaceCategory.LODGING, 2));

        // then
        server.verify();
    }

    @Test
    void 분류가_없으면_contentTypeId를_보내지_않는다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andExpect(request -> assertThat(request.getURI().getQuery()).doesNotContain("contentTypeId"))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        // when
        catalog.search(new PlaceSearchQuery("해운대", null, 1));

        // then
        server.verify();
    }

    @Test
    void 응답_항목을_검색_결과로_바꾼다() {
        // given
        respondWith(searchResponse(1, 1,
                item("126081", "12", "송정해수욕장", "부산광역시 해운대구 송정해변로 62", "129.1997", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.places()).containsExactly(new PlaceSearchItem(
                "126081", "송정해수욕장", PlaceCategory.ATTRACTION, "부산광역시 해운대구 송정해변로 62",
                new BigDecimal("35.1786"), new BigDecimal("129.1997")));
    }

    @Test
    void 주소가_비어_있으면_주소_없이_돌려준다() {
        // given
        respondWith(searchResponse(1, 1,
                item("126081", "12", "송정해수욕장", "", "129.1997", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.places()).singleElement()
                .satisfies(place -> assertThat(place.address()).isNull());
    }

    @Test
    void 결과가_없으면_빈_목록을_준다() {
        // given
        respondWith(emptyResponse());

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("없는장소", null, 1));

        // then
        assertThat(result.places()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 분류가_없거나_좌표가_없는_항목은_결과에서_뺀다() {
        // given
        respondWith(searchResponse(1, 3,
                item("1", "12", "송정해수욕장", "부산", "129.1997", "35.1786"),
                item("2", "15", "송정 축제", "부산", "129.2", "35.18"),
                item("3", "39", "좌표 없는 식당", "부산", "", "")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.places()).extracting(PlaceSearchItem::sourceId).containsExactly("1");
    }

    @Test
    void 단일_항목이_배열이_아닌_객체로_와도_결과로_바꾼다() {
        // given
        respondWith("""
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":%s},"numOfRows":20,"pageNo":1,"totalCount":1}}}
                """.formatted(item("126081", "12", "송정해수욕장", "부산", "129.1997", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.places()).extracting(PlaceSearchItem::sourceId).containsExactly("126081");
    }

    @Test
    void 좌표가_숫자가_아닌_항목은_결과에서_뺀다() {
        // given
        respondWith(searchResponse(1, 1,
                item("1", "12", "송정해수욕장", "부산", "unknown", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.places()).isEmpty();
    }

    @Test
    void 남은_결과가_있으면_다음_페이지가_있다고_알린다() {
        // given
        respondWith(searchResponse(1, 21,
                item("1", "12", "송정해수욕장", "부산", "129.1997", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 1));

        // then
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 마지막_페이지면_다음_페이지가_없다고_알린다() {
        // given
        respondWith(searchResponse(2, 40,
                item("1", "12", "송정해수욕장", "부산", "129.1997", "35.1786")));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 2));

        // then
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 페이지_번호가_커도_다음_페이지_여부를_올바르게_계산한다() {
        // given
        respondWith(searchResponse(200_000_000, 30));

        // when
        PlaceSearchResult result = catalog.search(new PlaceSearchQuery("송정", null, 200_000_000));

        // then
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 결과_코드가_정상이_아니면_검색_실패로_거부한다() {
        // given
        respondWith("""
                {"response":{"header":{"resultCode":"22","resultMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"}}}
                """);

        // when & then
        assertSearchFails(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

    @Test
    void 서비스_키가_등록되지_않았으면_검색_실패로_거부한다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("""
                        {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR",\
                        "returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}
                        """));

        // when & then
        assertSearchFails(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

    @Test
    void JSON이_아닌_오류_응답이면_검색_실패로_거부한다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andRespond(withSuccess("""
                        <OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>\
                        <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>\
                        <returnReasonCode>30</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>
                        """, MediaType.TEXT_XML));

        // when & then
        assertSearchFails(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

    @Test
    void 서버_오류면_검색_실패로_거부한다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        assertSearchFails(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

    @Test
    void 응답이_늦으면_검색_실패로_거부한다() {
        // given
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        // when & then
        assertSearchFails(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }

    @Test
    void 비활성화되어_있으면_호출하지_않고_검색_불가로_거부한다() {
        // given
        TourApiPlaceCatalog disabled = new TourApiPlaceCatalog(
                RestClient.builder().baseUrl(BASE_URL).build(),
                new TourApiProperties(false, URI.create(BASE_URL), null));

        // when & then
        assertThatThrownBy(() -> disabled.search(new PlaceSearchQuery("송정", null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_SEARCH_DISABLED));
    }

    private TourApiProperties enabledProperties() {
        return new TourApiProperties(true, URI.create(BASE_URL), SERVICE_KEY);
    }

    private void respondWith(String body) {
        server.expect(requestTo(startsWith(BASE_URL + "/searchKeyword2?")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void assertSearchFails(PlaceErrorCode expected) {
        assertThatThrownBy(() -> catalog.search(new PlaceSearchQuery("송정", null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    private static String encoded(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }

    // 실제 응답 기준: 결과가 없으면 items는 빈 문자열이고 numOfRows는 0이다.
    private static String emptyResponse() {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}}}
                """;
    }

    private static String searchResponse(int pageNo, int totalCount, String... items) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[%s]},"numOfRows":20,"pageNo":%d,"totalCount":%d}}}
                """.formatted(String.join(",", items), pageNo, totalCount);
    }

    private static String item(String contentId, String contentTypeId, String title,
                               String address, String mapx, String mapy) {
        return """
                {"contentid":"%s","contenttypeid":"%s","title":"%s","addr1":"%s","addr2":"","mapx":"%s","mapy":"%s"}
                """.formatted(contentId, contentTypeId, title, address, mapx, mapy);
    }

}
