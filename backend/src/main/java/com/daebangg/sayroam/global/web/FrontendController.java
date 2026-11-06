package com.daebangg.sayroam.global.web;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 화면 경로만 앱 셸로 연결한다. API·정적 파일·서버 경로는 Spring의 원래 처리를 따른다. */
@Controller
@Hidden
public class FrontendController {

    // 루트(/)는 Spring Boot의 기본 welcome page가 처리한다.
    @GetMapping(value = {"/brand", "/brand/", "/" + FrontendPaths.PAGE_SEGMENT,
            "/" + FrontendPaths.PAGE_SEGMENT + "/{*path}"}, produces = MediaType.TEXT_HTML_VALUE)
    public String index(HttpServletRequest request, HttpServletResponse response) throws NoResourceFoundException {
        // 화면 접두사 아래의 없는 파일도 HTML 200으로 바꾸지 않는다.
        if (FrontendPaths.path(request).contains(".")) {
            throw new NoResourceFoundException(HttpMethod.valueOf(request.getMethod()),
                    request.getRequestURI(), request.getServletPath());
        }
        if (!FrontendPaths.isKnownPage(FrontendPaths.path(request))) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
        }
        return "forward:/index.html";
    }
}
