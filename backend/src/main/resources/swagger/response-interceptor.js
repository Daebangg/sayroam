(response) => {
    const url = new URL(response.url, window.location.href);
    if (url.origin !== window.location.origin) return response;

    const path = url.pathname;
    const ui = window.ui;
    const accountChange = path === "/api/auth/password" || path === "/api/auth/account-deletion";
    if (response.status === 204 && (path === "/api/auth/logout" || accountChange)) {
        ui.authActions.logout(["bearerAuth"]);
        return response;
    }

    const tokenResponse = path === "/api/auth/login" || path === "/api/auth/refresh";
    if (!(tokenResponse && response.status === 200) && !(accountChange && response.status === 503)) {
        return response;
    }

    let body = response.obj ?? response.body ?? response.text;
    try {
        if (typeof body === "string") body = JSON.parse(body);
    } catch {
        return response;
    }

    if (tokenResponse && body?.tokenType === "Bearer"
            && typeof body.accessToken === "string" && body.accessToken.trim().length > 0) {
        // Swagger 메모리 상태에만 등록한다. Refresh 쿠키는 읽거나 별도로 저장하지 않는다.
        ui.preauthorizeApiKey("bearerAuth", body.accessToken);
    } else if (accountChange && (body?.code === "PASSWORD_CHANGE_UNAVAILABLE"
            || body?.code === "ACCOUNT_DELETION_UNAVAILABLE")) {
        // 서버가 Refresh 쿠키를 만료시키는 DB 결과 불확실 응답에서도 로컬 인증을 정리한다.
        ui.authActions.logout(["bearerAuth"]);
    }
    return response;
}
