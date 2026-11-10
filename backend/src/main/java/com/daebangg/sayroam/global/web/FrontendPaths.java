package com.daebangg.sayroam.global.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.util.Locale;
import java.util.Set;

/** 화면 URL과 서버·정적 자원 URL의 경계를 MVC와 Security에서 공유한다. */
public final class FrontendPaths {
    private static final String RESERVED_NAMES = "api|ws|assets|brand|fonts|images|error|actuator|swagger-ui|v3|webjars|login|logout|oauth2";
    public static final String PAGE_SEGMENT = "{page:(?!(?:" + RESERVED_NAMES + ")$)[^.]+}";
    private static final Set<String> RESERVED = Set.of(RESERVED_NAMES.split("\\|"));
    private static final Set<String> PAGES = Set.of("/", "/trips", "/matching", "/rooms", "/brand",
            "/settings", "/settings/appearance", "/settings/keyboard", "/settings/account", "/settings/profile");

    private FrontendPaths() {
    }

    public static boolean isPageRequest(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            return false;
        }
        String path = path(request);
        if ("/".equals(path) || "/brand".equals(path)) return true;
        String[] segments = path.split("/", 3);
        String first = segments.length > 1 ? segments[1] : "";
        return !first.isEmpty() && !RESERVED.contains(first);
    }

    public static String path(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.isEmpty() ? request.getRequestURI().substring(request.getContextPath().length()) : path;
    }

    public static boolean isKnownPage(String path) {
        String lowered = path.toLowerCase(Locale.ROOT);
        String normalized = lowered.endsWith("/") && lowered.length() > 1
                ? lowered.substring(0, lowered.length() - 1) : lowered;
        return PAGES.contains(normalized);
    }
}
