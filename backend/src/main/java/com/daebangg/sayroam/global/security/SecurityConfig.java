package com.daebangg.sayroam.global.security;

import com.daebangg.sayroam.global.web.FrontendPaths;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

    // 공개 정적 자원·SPA 주소 목록을 GET·HEAD 규칙에서 공유한다.
    private static final String[] PUBLIC_FRONTEND_PATHS = {
            "/", "/index.html", "/assets/**", "/brand/**", "/fonts/**", "/images/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper,
                                          JwtDecoder accessTokenDecoder,
                                          ActiveAccountJwtConverter accountConverter) throws Exception {
        var problemHandler = new SecurityProblemHandler(jsonMapper);
        var jwtProvider = new JwtAuthenticationProvider(accessTokenDecoder);
        jwtProvider.setJwtAuthenticationConverter(accountConverter);
        var bearerConverter = new BearerTokenAuthenticationConverter();
        // 만료·변조된 Access Token이 함께 전달돼도 쿠키 기반 갱신·로그아웃을 가로막지 않는다.
        var bearerFilter = new BearerTokenAuthenticationFilter(new ProviderManager(jwtProvider),
                request -> HttpMethod.POST.matches(request.getMethod())
                        && ("/api/auth/refresh".equals(request.getServletPath())
                        || "/api/auth/logout".equals(request.getServletPath()))
                        ? null : bearerConverter.convert(request));
        bearerFilter.setAuthenticationEntryPoint(problemHandler);
        var failureHandler = new AuthenticationEntryPointFailureHandler(problemHandler);
        failureHandler.setRethrowAuthenticationServiceException(false);
        bearerFilter.setAuthenticationFailureHandler(failureHandler);
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // auth는 로그인·갱신·로그아웃 쿠키 경로이므로 Bearer 헤더가 있어도 CSRF를 검사한다.
                // 나머지 API는 Authorization 헤더만 인증 수단으로 사용한다.
                .csrf(csrf -> csrf.spa().requireCsrfProtectionMatcher(request -> {
                    String path = request.getServletPath();
                    return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)
                            && (path.equals("/api/auth") || path.startsWith("/api/auth/"));
                }))
                // 표준 필터를 직접 연결해 Resource Server의 자동 Bearer CSRF 제외가 auth에 적용되지 않게 한다.
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // 내부 오류 디스패치가 인증 오류로 덮이지 않도록 허용한다.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // 공개 화면과 빌드된 정적 자원만 허용한다. API 접근 규칙은 아래에서 유지한다.
                        .requestMatchers(FrontendPaths::isPageRequest).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_FRONTEND_PATHS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_FRONTEND_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        // 브라우저 WebSocket은 핸드셰이크에 Authorization을 넣을 수 없어 STOMP CONNECT에서 인증한다.
                        .requestMatchers(HttpMethod.GET, "/ws").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/email-verifications",
                                "/api/auth/email-verifications/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/social/{provider}/authorizations",
                                "/api/auth/social/results/exchange", "/api/auth/social/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/social/{provider}/callback").permitAll()
                        .requestMatchers("/api/admin", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler)
                );

        return http.build();
    }
}
