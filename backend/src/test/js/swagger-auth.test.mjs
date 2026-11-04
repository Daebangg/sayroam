import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import vm from "node:vm";

const source = readFileSync(new URL("../../main/resources/swagger/response-interceptor.js", import.meta.url), "utf8");
const origin = "http://localhost:8080";

function swagger() {
    const calls = [];
    const forbiddenStorage = new Proxy({}, { get() { throw new Error("Tokens must stay in Swagger memory"); } });
    const window = {
        location: { origin, href: `${origin}/swagger-ui/index.html` },
        localStorage: forbiddenStorage,
        sessionStorage: forbiddenStorage,
        ui: {
            preauthorizeApiKey: (scheme, value) => calls.push(["authorize", scheme, value]),
            authActions: { logout: schemes => calls.push(["logout", ...schemes]) },
        },
    };
    return { calls, intercept: vm.runInNewContext(`(${source})`, { window, URL }) };
}

test("successful login registers only the access token and returns the original response", () => {
    const ui = swagger();
    const response = { url: `${origin}/api/auth/login`, status: 200,
        obj: { accessToken: "access-1", tokenType: "Bearer", refreshToken: "never-copy" } };
    assert.equal(ui.intercept(response), response);
    assert.deepEqual(ui.calls, [["authorize", "bearerAuth", "access-1"]]);
});

test("successful refresh replaces the previously registered access token", () => {
    const ui = swagger();
    ui.intercept({ url: "/api/auth/login", status: 200, obj: { accessToken: "old", tokenType: "Bearer" } });
    ui.intercept({ url: "/api/auth/refresh", status: 200, text: JSON.stringify({ accessToken: "new", tokenType: "Bearer" }) });
    assert.deepEqual(ui.calls, [["authorize", "bearerAuth", "old"], ["authorize", "bearerAuth", "new"]]);
});

test("failed login or refresh leaves the current authorization unchanged", () => {
    const ui = swagger();
    for (const path of ["login", "refresh"]) {
        for (const status of [400, 401, 403, 500, 503]) {
            ui.intercept({ url: `/api/auth/${path}`, status, obj: { accessToken: "ignored", tokenType: "Bearer" } });
        }
    }
    assert.deepEqual(ui.calls, []);
});

test("unrelated endpoints and cross-origin responses cannot replace or clear authorization", () => {
    const ui = swagger();
    for (const url of ["/api/users/me", "/api/auth/login/extra", "https://other.example/api/auth/login"]) {
        ui.intercept({ url, status: 200, obj: { accessToken: "ignored", tokenType: "Bearer" } });
    }
    ui.intercept({ url: "https://other.example/api/auth/logout", status: 204 });
    assert.deepEqual(ui.calls, []);
});

test("malformed or incomplete token responses cannot overwrite authorization", () => {
    const ui = swagger();
    for (const body of ["not-json", "null", "{}", '{"accessToken":"","tokenType":"Bearer"}',
        '{"accessToken":"  ","tokenType":"Bearer"}', '{"accessToken":"value","tokenType":"Refresh"}']) {
        ui.intercept({ url: "/api/auth/login", status: 200, body });
    }
    assert.deepEqual(ui.calls, []);
});

test("completed logout, password change and account deletion clear the Swagger bearer token", () => {
    for (const path of ["logout", "password", "account-deletion"]) {
        const ui = swagger();
        const response = { url: `/api/auth/${path}`, status: 204 };
        assert.equal(ui.intercept(response), response);
        assert.deepEqual(ui.calls, [["logout", "bearerAuth"]]);
    }
});

test("an unavailable logout keeps authorization for an explicit retry", () => {
    const ui = swagger();
    ui.intercept({ url: "/api/auth/logout", status: 503, obj: { code: "AUTHENTICATION_UNAVAILABLE" } });
    assert.deepEqual(ui.calls, []);
});

test("uncertain account commits clear local authorization while earlier failures retain it", () => {
    for (const [path, code] of [["password", "PASSWORD_CHANGE_UNAVAILABLE"], ["account-deletion", "ACCOUNT_DELETION_UNAVAILABLE"]]) {
        const ui = swagger();
        ui.intercept({ url: `/api/auth/${path}`, status: 503, obj: { code: "EMAIL_VERIFICATION_UNAVAILABLE" } });
        assert.deepEqual(ui.calls, []);
        ui.intercept({ url: `/api/auth/${path}`, status: 503, obj: { code } });
        assert.deepEqual(ui.calls, [["logout", "bearerAuth"]]);
    }
});
