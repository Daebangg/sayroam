package com.daebangg.sayroam.global.security;

import com.daebangg.sayroam.global.exception.ApiProblems;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

public final class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper jsonMapper;

    public SecurityProblemHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        boolean unavailable = exception instanceof AuthenticationServiceException;
        if (!unavailable) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        write(request, response, unavailable
                ? CommonErrorCode.AUTHENTICATION_UNAVAILABLE : CommonErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, exception instanceof CsrfException
                ? CommonErrorCode.INVALID_CSRF_TOKEN : CommonErrorCode.ACCESS_DENIED);
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       CommonErrorCode code) throws IOException {
        var problem = ApiProblems.of(code);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(code.getStatus().value());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
