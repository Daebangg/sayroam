package com.daebangg.sayroam.global.web;

import com.daebangg.sayroam.global.logging.LogContext;
import com.daebangg.sayroam.global.logging.SafeLog;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Security 바깥에서 완료를 기록한다. 본문·원문 URL·헤더 값은 읽지 않는다. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String STATE_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".state";
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> POLLING_ROUTES = Set.of("/api/notifications", "/api/notifications/summary",
            "/api/rooms/{roomId}/messages/summary");

    private final long slowRequestMs;

    public RequestLoggingFilter(@Value("${app.logging.http.slow-request-ms:1000}") long slowRequestMs) {
        if (slowRequestMs < 0) throw new IllegalArgumentException("app.logging.http.slow-request-ms must be nonnegative");
        this.slowRequestMs = slowRequestMs;
    }

    public static void recordError(HttpServletRequest request, String errorCode, Throwable failure) {
        if (request.getAttribute(STATE_ATTRIBUTE) instanceof RequestState state) {
            state.errorCode = errorCode;
            if (failure != null) state.failure = failure;
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        doFilterInternal(request, response, chain);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RequestState state;
        if (request.getAttribute(STATE_ATTRIBUTE) instanceof RequestState existing) {
            state = existing;
        } else {
            state = new RequestState();
            request.setAttribute(STATE_ATTRIBUTE, state);
        }
        response.setHeader(REQUEST_ID_HEADER, state.requestId);
        try (LogContext ignored = LogContext.request(state.requestId)) {
            try {
                chain.doFilter(new AsyncTrackingRequest(request, response, state), response);
            } catch (IOException | ServletException | RuntimeException exception) {
                state.failure = exception;
                state.failed = true;
                throw exception;
            } finally {
                if (!state.async) complete(request, response, state);
            }
        }
    }

    private void complete(HttpServletRequest request, HttpServletResponse response, RequestState state) {
        if (!state.completed.compareAndSet(false, true)) return;
        try (LogContext ignored = LogContext.request(state.requestId)) {
            int status = response.getStatus();
            if (status < 400 && state.failed) status = 500;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - state.startedAt);
            String method = METHODS.contains(request.getMethod()) ? request.getMethod() : "OTHER";
            // Spring이 등록한 템플릿만 사용한다. 보안 필터에서 거절되거나 없는 경로는 원문을 남기지 않는다.
            String route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) instanceof String pattern
                    ? pattern : "UNMAPPED";
            boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");
            boolean quiet = !api || "OPTIONS".equals(method)
                    || ("GET".equals(method) && POLLING_ROUTES.contains(route));
            LoggingEventBuilder event = status >= 500 ? SafeLog.withFailure(log.atError(), state.failure)
                    : quiet && status < 400 && durationMs < slowRequestMs ? log.atDebug() : log.atInfo();
            event.addKeyValue("event", "http_request_completed")
                    .addKeyValue("method", method)
                    .addKeyValue("route", route)
                    .addKeyValue("status", status)
                    .addKeyValue("durationMs", durationMs);
            if (state.errorCode != null) event.addKeyValue("errorCode", state.errorCode);
            if (state.timedOut) event.addKeyValue("timedOut", true);
            event.log("HTTP request completed");
        }
    }

    private final class AsyncTrackingRequest extends HttpServletRequestWrapper {
        private final HttpServletResponse response;
        private final RequestState state;

        private AsyncTrackingRequest(HttpServletRequest request, HttpServletResponse response, RequestState state) {
            super(request);
            this.response = response;
            this.state = state;
        }

        @Override
        public AsyncContext startAsync() {
            return startAsync(this, response);
        }

        @Override
        public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
            AsyncContext context = super.startAsync(request, response);
            if (!state.async) {
                state.async = true;
                context.addListener(new CompletionListener(this, this.response, state));
            }
            return context;
        }
    }

    private final class CompletionListener implements AsyncListener {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final RequestState state;

        private CompletionListener(HttpServletRequest request, HttpServletResponse response, RequestState state) {
            this.request = request;
            this.response = response;
            this.state = state;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            complete(request, response, state);
        }

        @Override
        public void onError(AsyncEvent event) {
            state.failed = true;
            if (event.getThrowable() != null) state.failure = event.getThrowable();
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            state.timedOut = true;
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }

    private static final class RequestState {
        private final String requestId = UUID.randomUUID().toString();
        private final long startedAt = System.nanoTime();
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile boolean async;
        private volatile boolean failed;
        private volatile boolean timedOut;
        private volatile Throwable failure;
        private volatile String errorCode;
    }
}
