package com.daebangg.sayroam.place.infrastructure.client;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * 한국관광공사 TourAPI 연결 설정. 서비스 키가 없는 환경은 장소 검색을 끈다.
 */
@Validated
@ConfigurationProperties("app.place.tour-api")
public record TourApiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://apis.data.go.kr/B551011/KorService2") URI baseUrl,
        String serviceKey
) {

    @AssertTrue(message = "An enabled TourAPI requires service-key")
    public boolean isConfigured() {
        return !enabled || (serviceKey != null && !serviceKey.isBlank());
    }

    @Override
    public String toString() {
        return "TourApiProperties[enabled=" + enabled + ", baseUrl=" + baseUrl + ", serviceKey=redacted]";
    }

}
