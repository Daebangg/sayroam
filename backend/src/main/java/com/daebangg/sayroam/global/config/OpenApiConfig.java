package com.daebangg.sayroam.global.config;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer authCsrfSecurity() {
        // 필수 입력 파라미터 대신 보안 조건으로 표시해 Swagger의 자동 헤더 전달을 허용한다.
        // 기존 Bearer/Refresh 조건과 같은 객체에 넣어 OR가 아닌 AND 조건을 유지한다.
        var safeMethods = Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.HEAD,
                PathItem.HttpMethod.OPTIONS, PathItem.HttpMethod.TRACE);
        return api -> api.getPaths().forEach((path, item) -> {
            if (!path.equals("/api/auth") && !path.startsWith("/api/auth/")) return;
            item.readOperationsMap().forEach((method, operation) -> {
                if (safeMethods.contains(method)) return;
                var requirements = operation.getSecurity();
                if (requirements == null || requirements.isEmpty()) {
                    operation.setSecurity(List.of(new SecurityRequirement().addList("csrfToken")));
                } else {
                    requirements.forEach(requirement -> requirement.addList("csrfToken"));
                }
            });
        });
    }

    @Bean
    OpenAPI openAPI(Optional<BuildProperties> buildProperties) {
        var errorCode = new StringSchema();
        errorCode.setEnum(Stream.of(CommonErrorCode.values(), AuthErrorCode.values(),
                        RoomErrorCode.values(), UserErrorCode.values())
                .flatMap(Arrays::stream).map(ErrorCode::getCode).distinct().sorted().toList());
        errorCode.setDescription("백엔드에 정의된 업무·공통 오류 코드. Spring 기본 요청 오류와 향후 추가 코드는 별도 문자열로 받을 수 있습니다.");
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
                .addProperty("code", new Schema<>().addAnyOfItem(new Schema<>().$ref("#/components/schemas/ApiErrorCode"))
                        .addAnyOfItem(new StringSchema().description("Spring HTTP 상태 이름·HTTP_<상태 코드> 및 알 수 없는 향후 코드"))
                        .example("UNAUTHENTICATED"))
                .addProperty("retryAfterSeconds", new IntegerSchema().format("int64")
                        .description("재시도 대기가 있는 429 응답에만 포함하는 대기 초. Retry-After 헤더와 같은 값"))
                .addProperty("errors", new ArraySchema().items(new Schema<>().$ref("#/components/schemas/FieldError")));
        problem.setRequired(List.of("title", "status", "detail", "instance", "code"));
        var components = new Components()
                .addSecuritySchemes("csrfToken", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN")
                        .description("XSRF-TOKEN 쿠키의 원문을 전달합니다. local Swagger UI는 쿠키에서 자동 입력하므로 Authorize에 입력할 필요가 없습니다. 일반 클라이언트는 GET /api/auth/csrf로 쿠키를 준비한 뒤 헤더를 직접 전달해야 합니다."))
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("local Swagger는 로그인·갱신 성공 응답의 Access Token을 자동 등록합니다. 직접 등록할 때는 Bearer 접두사 없이 원문만 입력합니다. Authorization: Bearer 헤더만 지원하며 쿠키·쿼리·본문 토큰은 사용하지 않습니다. auth 변경 요청은 별도로 CSRF 쿠키·헤더가 필요합니다."))
                .addSecuritySchemes("refreshCookie", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("sayroam_refresh")
                        .description("로그인에서 설정하는 HttpOnly Refresh 쿠키. 브라우저가 같은 호스트의 갱신·로그아웃 요청에 자동 전송합니다. Authorize에 값을 수동 입력하는 방식이 아니며, XSRF-TOKEN 쿠키와 X-XSRF-TOKEN 헤더도 필요합니다. 갱신·로그아웃에서는 Authorization 헤더를 사용하지 않습니다. 로그아웃은 Refresh 쿠키가 없어도 브라우저 정리를 허용합니다."))
                .addSchemas("ApiProblem", problem)
                .addSchemas("ApiErrorCode", errorCode)
                .addSchemas("FieldError", fieldError)
                .addResponses("BadRequest", problemResponse("400: INVALID_INPUT 또는 Spring 요청 오류. 검증 실패 시 errors 포함"))
                .addResponses("UserProfileBadRequest", problemResponse("400: USER_NICKNAME_REQUIRED, USER_NICKNAME_TOO_LONG, USER_NICKNAME_INVALID, USER_PROFILE_IMAGE_URL_TOO_LONG, USER_PROFILE_IMAGE_URL_INVALID, USER_BIRTH_YEAR_INVALID 또는 Spring 요청 오류. 허용 필드는 nickname·profileImageUrl·gender·birthYear이며 보호 필드와 잘못된 JSON 타입은 거절"))
                .addResponses("SocialBadRequest", problemResponse("400: SOCIAL_PROVIDER_DISABLED, SOCIAL_AUTH_INVALID, SOCIAL_AUTH_CANCELLED, SOCIAL_RETURN_PATH_INVALID, SOCIAL_SIGNUP_INVALID, SIGNUP_PROOF_INVALID 또는 INVALID_INPUT. 만료·소비된 상태나 잘못된 브라우저이면 소셜 인증부터 다시 시작"))
                .addResponses("SocialUnauthorized", problemResponse("401: SOCIAL_AUTH_FAILED 또는 AUTHENTICATION_REVOKED. 공급자 인증 실패·기존 로그인 수단/인증 버전 변경. 새 소셜 인증 필요"))
                .addResponses("SocialConflict", problemResponse("409: EMAIL_ALREADY_REGISTERED, SOCIAL_ACCOUNT_CONFLICT 또는 SIGNUP_PROOF_ALREADY_USED. 이메일 자동 병합 없음. 기존 회원은 기존 계정으로 로그인 후 명시적 연결 필요"))
                .addResponses("SocialUnavailable", problemResponse("503: SOCIAL_AUTH_UNAVAILABLE, AUTHENTICATION_UNAVAILABLE 또는 EMAIL_VERIFICATION_UNAVAILABLE. 상태·증명 소비나 가입 커밋 결과가 불확실할 수 있음. 자동 재시도 없이 소셜 인증부터 다시 시작"))
                .addResponses("Unauthorized", problemResponse("401: UNAUTHENTICATED. 토큰 누락·만료·변조·잘못된 발급자/대상·유형·인증 버전 또는 탈퇴·삭제 회원. WWW-Authenticate: Bearer"))
                .addResponses("AccountAuthenticationRevoked", problemResponse("401: UNAUTHENTICATED 또는 AUTHENTICATION_REVOKED. 현재 DB 인증 버전과 다른 로그인은 거절. 재로그인 필요. WWW-Authenticate: Bearer"))
                .addResponses("PasswordChangeBadRequest", problemResponse("400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. PASSWORD_CHANGE 증명과 8~128자의 새 비밀번호 필요. 비밀번호 정책 실패는 증명을 소비하지 않음"))
                .addResponses("PasswordChangeForbidden", problemResponse("403: ACCESS_DENIED, INVALID_CSRF_TOKEN, ACCOUNT_VERIFICATION_NOT_ALLOWED 또는 PASSWORD_CHANGE_NOT_ALLOWED. 활성·이메일 인증·기존 비밀번호 등록 계정만 허용. 소셜 전용 계정의 비밀번호 등록은 지원하지 않음"))
                .addResponses("PasswordChangeConflict", problemResponse("409: ACCOUNT_PROOF_ALREADY_USED 또는 PASSWORD_CHANGE_CONFLICT. 소비된 증명 복원 없음. 회원 상태·이메일·인증 버전 충돌 시 재로그인 후 새 본인 확인 필요"))
                .addResponses("AccountProofExpired", problemResponse("410: ACCOUNT_PROOF_EXPIRED. 없거나 만료된 증명 또는 현재 회원·이메일·목적·인증 버전의 증명이 없음. 새 본인 확인 필요"))
                .addResponses("AccountDeletionBadRequest", problemResponse("400: INVALID_INPUT, ACCOUNT_PROOF_INVALID 또는 Spring 요청 오류. 현재 회원의 ACCOUNT_DELETION 증명 필요"))
                .addResponses("AccountDeletionConflict", problemResponse("409: ACCOUNT_PROOF_ALREADY_USED 또는 ACCOUNT_DELETION_CONFLICT. 소비한 증명은 복원하지 않음. 활성 계정이 남아 있을 때만 재로그인 후 새 본인 확인으로 재시도"))
                .addResponses("AccountDeletionUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE(사전 회원 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 ACCOUNT_DELETION_UNAVAILABLE(DB 커밋 결과 불확실). 마지막 코드일 때만 Refresh 쿠키 만료. 로컬 로그인 상태 정리 후 완료 여부 불확실 상태를 표시하며 자동 재전송·자동 재가입하지 않음")
                        .addHeaderObject("Set-Cookie", new io.swagger.v3.oas.models.headers.Header()
                                .description("ACCOUNT_DELETION_UNAVAILABLE일 때만 sayroam_refresh 만료. 증명/사전 조회 단계의 503에서는 설정하지 않음")
                                .schema(new StringSchema())))
                .addResponses("PasswordChangeUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE(사전 DB 조회), EMAIL_VERIFICATION_UNAVAILABLE(증명 소비 결과 불확실) 또는 PASSWORD_CHANGE_UNAVAILABLE(DB 변경·커밋 결과 불확실). 자동 재시도 금지. 마지막 코드일 때만 Refresh 쿠키를 만료시키며 프론트는 로그인 상태를 지우고 재로그인으로 결과 확인")
                        .addHeaderObject("Set-Cookie", new io.swagger.v3.oas.models.headers.Header()
                                .description("PASSWORD_CHANGE_UNAVAILABLE일 때 sayroam_refresh 만료. 나머지 503에서는 설정하지 않음")
                                .schema(new StringSchema())))
                .addResponses("LoginUnauthorized", problemResponse("401: INVALID_CREDENTIALS. 이메일 없음·비밀번호 불일치·탈퇴·이메일 미인증·비밀번호 미등록에 동일한 응답. 불필요한 Authorization 헤더가 있으면 필터에서 UNAUTHENTICATED로 거절될 수 있음"))
                .addResponses("AuthenticationUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE. 회원 DB 또는 Refresh Redis 저장소 장애. 성공 응답 없이 거절하며 갱신 결과가 불확실할 수 있으므로 자동 재시도하지 않음"))
                .addResponses("RefreshUnauthorized", problemResponse("401: REFRESH_TOKEN_INVALID 또는 REFRESH_TOKEN_REUSED. 쿠키 누락·중복·형식 오류·만료·폐기·회원 삭제/탈퇴·교체 한도, 또는 사용한 토큰 재사용. Refresh 쿠키를 만료시키며 재로그인 필요. 재사용은 해당 로그인만 갱신 불가로 만듦")
                        .addHeaderObject("Set-Cookie", new io.swagger.v3.oas.models.headers.Header()
                                .description("sayroam_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict. Secure 설정은 발급과 동일")
                                .schema(new StringSchema())))
                .addResponses("LogoutCookieInvalid", problemResponse("401: REFRESH_TOKEN_INVALID. 같은 이름의 Refresh 쿠키가 중복되어 폐기 대상을 선택할 수 없음. 발급 경로의 쿠키만 만료시키며 다른 경로·호스트의 중복 쿠키는 브라우저에서 정리해야 함")
                        .addHeaderObject("Set-Cookie", new io.swagger.v3.oas.models.headers.Header()
                                .description("sayroam_refresh 만료. 발급과 같은 경로·속성으로 Max-Age=0")
                                .schema(new StringSchema())))
                .addResponses("LogoutUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE. Redis 폐기 결과를 확인할 수 없음. 성공 응답·Refresh 쿠키 삭제 없이 실패하며 같은 쿠키로 재시도 가능. 폐기는 이미 실행됐을 수도 있음"))
                .addResponses("Forbidden", problemResponse("403: ACCESS_DENIED 또는 INVALID_CSRF_TOKEN"))
                .addResponses("AccountVerificationForbidden", problemResponse("403: ACCESS_DENIED, INVALID_CSRF_TOKEN 또는 ACCOUNT_VERIFICATION_NOT_ALLOWED. 활성 계정의 인증된 대표 이메일이 필요"))
                .addResponses("AccountVerificationUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE, EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 회원 DB·SMTP 설정·Redis 문제. 발급 결과가 불확실하면 자동 재시도하지 않고 제한 시간 이후 새 코드 요청"))
                .addResponses("AccountConfirmationUnavailable", problemResponse("503: AUTHENTICATION_UNAVAILABLE 또는 EMAIL_VERIFICATION_UNAVAILABLE. 코드 소비·증명 발급 결과가 불확실할 수 있어 자동 재시도하지 않고 새 코드 요청. SMTP 없이 기존 코드 확인 가능"))
                .addResponses("InternalServerError", problemResponse("500: INTERNAL_SERVER_ERROR"))
                .addResponses("EmailRequestSuperseded", problemResponse("409: EMAIL_REQUEST_SUPERSEDED. 만료·교체된 요청이며 최신 요청을 사용해야 함"))
                .addResponses("EmailRateLimited", problemResponse("429: EMAIL_RATE_LIMITED. Retry-After 헤더와 retryAfterSeconds 이후 재요청")
                        .addHeaderObject("Retry-After", new io.swagger.v3.oas.models.headers.Header()
                                .description("재시도 전 대기할 초")
                                .schema(new IntegerSchema().format("int64"))))
                .addResponses("EmailDeliveryFailed", problemResponse("502: EMAIL_DELIVERY_FAILED. 코드 무효화, 발송 제한 유지. 자동 재시도 없음"))
                .addResponses("EmailVerificationUnavailable", problemResponse("503: EMAIL_DELIVERY_DISABLED 또는 EMAIL_VERIFICATION_UNAVAILABLE. 설정·저장소 복구 후 재요청"))
                .addResponses("EmailCodeInvalid", problemResponse("400: INVALID_INPUT, EMAIL_CODE_INVALID 또는 Spring 요청 오류. 형식 오류는 수정, 코드 불일치는 남은 횟수 안에서 재입력"))
                .addResponses("EmailVerificationConflict", problemResponse("409: EMAIL_REQUEST_SUPERSEDED 또는 EMAIL_CODE_ALREADY_USED. 최신 요청 또는 이미 받은 증명을 사용"))
                .addResponses("EmailCodeExpired", problemResponse("410: EMAIL_CODE_EXPIRED. 없거나 만료된 코드. 새 코드 발급 필요"))
                .addResponses("EmailCodeAttemptsExceeded", problemResponse("429: EMAIL_CODE_ATTEMPTS_EXCEEDED. 이 코드는 더 사용할 수 없으며 새 코드 발급 필요. Retry-After는 제공하지 않음"))
                .addResponses("EmailConfirmationUnavailable", problemResponse("503: EMAIL_VERIFICATION_UNAVAILABLE. Redis 작업 결과가 불확실할 수 있음. 코드가 소비됐으면 새 코드 발급 필요"))
                .addResponses("SignupBadRequest", problemResponse("400: INVALID_INPUT, PASSWORD_POLICY_VIOLATION, SIGNUP_PROOF_INVALID 또는 Spring 요청 오류. 입력·비밀번호 정책 검증 실패는 증명을 소비하지 않음"))
                .addResponses("SignupConflict", problemResponse("409: SIGNUP_PROOF_ALREADY_USED 또는 EMAIL_ALREADY_REGISTERED. 이미 소비한 증명은 복구하지 않음"))
                .addResponses("SignupProofExpired", problemResponse("410: SIGNUP_PROOF_EXPIRED. 없거나 만료된 증명. 새 이메일 인증 필요"))
                .addResponses("SignupUnavailable", problemResponse("503: EMAIL_VERIFICATION_UNAVAILABLE 또는 SIGNUP_UNAVAILABLE. 증명 소비·DB 커밋 결과가 불확실할 수 있음. 자동 재시도 없이 새 인증 필요; 계정은 이미 생성됐을 수도 있음"))
                .addResponses("RoomAccessDenied", problemResponse("403: ROOM_ACCESS_DENIED. 없는 방이거나 현재 참여 중인 멤버가 아님"))
                .addResponses("RoomBadRequest", problemResponse("400: ROOM_NAME_TOO_LONG 또는 Spring 요청 오류. 이름을 20자 이내로 수정 후 재요청"));
        return new OpenAPI().components(components).servers(List.of(new Server().url("/")))
                .info(new Info().title("Sayroam API").version(buildProperties.map(BuildProperties::getVersion).orElse("dev")));
    }

    private ApiResponse problemResponse(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
