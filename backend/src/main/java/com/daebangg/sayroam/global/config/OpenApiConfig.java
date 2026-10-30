package com.daebangg.sayroam.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI(Optional<BuildProperties> buildProperties) {
        // ProblemDetail의 동적 확장 필드도 프론트엔드 타입 생성기에 명시적으로 노출한다.
        Schema<Object> fieldError = new Schema<>();
        fieldError.setType("object");
        fieldError
                .addProperty("field", new StringSchema())
                .addProperty("reason", new StringSchema());
        fieldError.setRequired(List.of("field", "reason"));
        Schema<Object> problem = new Schema<>();
        problem.setType("object");
        problem
                .addProperty("type", new StringSchema().format("uri").description("생략하면 about:blank로 해석합니다."))
                .addProperty("title", new StringSchema().example("Unauthorized"))
                .addProperty("status", new IntegerSchema().example(401))
                .addProperty("detail", new StringSchema().example("Authentication is required"))
                .addProperty("instance", new StringSchema().format("uri-reference").example("/api/users/me"))
                .addProperty("code", new StringSchema().example("UNAUTHENTICATED"))
                .addProperty("retryAfterSeconds", new IntegerSchema().format("int64")
                        .description("429 응답의 재시도 대기 초. Retry-After 헤더와 같은 값"))
                .addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/FieldError")));
        problem.setRequired(List.of("title", "status", "detail", "instance", "code"));
        var components = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("#30에서 도입 예정. 현재 토큰 발급·검증 미구현이며 임의 토큰으로 인증되지 않습니다."))
                .addSchemas("ApiProblem", problem)
                .addSchemas("FieldError", fieldError)
                .addResponses("BadRequest", problemResponse("400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함"))
                .addResponses("Unauthorized", problemResponse("401: UNAUTHENTICATED"))
                .addResponses("Forbidden", problemResponse("403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN"))
                .addResponses("InternalServerError", problemResponse("500: INTERNAL_SERVER_ERROR"))
                .addResponses("EmailRequestSuperseded", problemResponse("409: EMAIL_REQUEST_SUPERSEDED. 만료·교체된 요청이며 최신 요청을 사용해야 함"))
                .addResponses("EmailRateLimited", problemResponse("429: EMAIL_RATE_LIMITED. Retry-After 헤더와 retryAfterSeconds 이후 재요청")
                        .addHeaderObject("Retry-After", new io.swagger.v3.oas.models.headers.Header()
                                .description("재시도 전 대기할 초")
                                .schema(new IntegerSchema().format("int64"))))
                .addResponses("EmailDeliveryFailed", problemResponse("502: EMAIL_DELIVERY_FAILED. 코드 무효화, 발송 제한 유지. 자동 재시도 없음"))
                .addResponses("EmailVerificationUnavailable", problemResponse("503: EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 설정·저장소 복구 후 재요청"));
        return new OpenAPI().components(components).servers(List.of(new Server().url("/")))
                .info(new Info().title("Sayroam API").version(buildProperties.map(BuildProperties::getVersion).orElse("dev")));
    }

    private ApiResponse problemResponse(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
