package com.daebangg.sayroam.global.config;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/** 로컬 문서 UI의 편의 기능이며 서버의 인증·CSRF 정책은 변경하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("local")
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
public class SwaggerUiAuthConfig {
    @Bean
    SwaggerIndexTransformer swaggerAuthIndexTransformer(SwaggerUiConfigProperties properties,
            SwaggerUiOAuthProperties oauth, SwaggerWelcomeCommon welcome, ObjectMapperProvider mapper) throws IOException {
        return new SwaggerAuthIndexTransformer(properties, oauth, welcome, mapper);
    }
}
