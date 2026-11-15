package com.daebangg.sayroam.place.infrastructure.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 검색 요청이 TourAPI 지연에 오래 묶이지 않도록 연결·응답 대기 시간을 제한한다.
 */
@Configuration(proxyBeanMethods = false)
public class TourApiClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    RestClient tourApiRestClient(TourApiProperties properties) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(CONNECT_TIMEOUT);
        requests.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requests)
                .build();
    }

}
