package com.daebangg.sayroam.global.config;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Springdoc의 CSRF 등 기본 변환을 먼저 적용하고 응답 처리만 추가한다. */
public class SwaggerAuthIndexTransformer extends SwaggerIndexPageTransformer {
    private static final String PRESETS = "presets: [";
    private final String responseInterceptor;

    public SwaggerAuthIndexTransformer(SwaggerUiConfigProperties properties, SwaggerUiOAuthProperties oauth,
            SwaggerWelcomeCommon welcome, ObjectMapperProvider mapper) throws IOException {
        super(properties, oauth, welcome, mapper);
        responseInterceptor = new ClassPathResource("swagger/response-interceptor.js")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Override
    protected String defaultTransformations(SwaggerUiConfigParameters parameters, InputStream input) throws IOException {
        String initializer = super.defaultTransformations(parameters, input);
        if (!initializer.contains(PRESETS)) {
            throw new IOException("Swagger initializer does not contain the expected presets marker");
        }
        return initializer.replace(PRESETS, "responseInterceptor: " + responseInterceptor + ",\n    " + PRESETS);
    }
}
