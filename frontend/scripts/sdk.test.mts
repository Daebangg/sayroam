/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { clearAccessToken, getAccessToken, setAccessToken } from "../src/api/auth/tokenStore.ts";
import { create, login, logout, me, refresh } from "../src/api/sdk.ts";
import { getCurrentUser } from "../src/api/users/userApi.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const token = { accessToken: "sdk-token", tokenType: "Bearer", expiresIn: 900 } as const;
const user = { userId: 1, email: null, nickname: "SDK", profileImageUrl: null, role: "USER" };

beforeEach(() => {
    clearAccessToken();
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=csrf-sdk" },
    });
});
afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("생성 로그인 함수는 URL·POST·JSON·CSRF를 구성하고 Bearer를 제외", async (t) => {
    setAccessToken(token);
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/auth/login");
        assert.equal(options?.method, "POST");
        const headers = new Headers(options?.headers);
        assert.equal(headers.get("Authorization"), null);
        assert.equal(headers.get("X-XSRF-TOKEN"), "csrf-sdk");
        assert.deepEqual(JSON.parse(String(options?.body)), {
            email: "user@example.test",
            password: "password",
        });
        return Response.json(token);
    });
    assert.deepEqual(
        await login({ body: { email: "user@example.test", password: "password" } }),
        token,
    );
    assert.equal(getAccessToken(), "sdk-token");
});

test("방 생성도 명세의 Bearer 요구를 자동 적용하고 쿠키 준비 GET은 생략", async (t) => {
    setAccessToken(token);
    const fetchMock = t.mock.method(
        globalThis,
        "fetch",
        async (path: unknown, options?: RequestInit) => {
            assert.equal(path, "/api/rooms");
            assert.equal(options?.method, "POST");
            const headers = new Headers(options?.headers);
            assert.equal(headers.get("Authorization"), "Bearer sdk-token");
            assert.equal(headers.get("X-XSRF-TOKEN"), null);
            return Response.json({ roomId: 123 });
        },
    );
    assert.deepEqual(await create({ body: { name: "여행" } }), { roomId: 123 });
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("생성 Refresh 함수는 쿠키 전송과 CSRF만 사용하고 토큰 헤더는 추가하지 않음", async (t) => {
    setAccessToken(token);
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/auth/refresh");
        assert.equal(options?.credentials, "same-origin");
        const headers = new Headers(options?.headers);
        assert.equal(headers.get("Authorization"), null);
        assert.equal(headers.get("Cookie"), null);
        assert.equal(headers.get("X-XSRF-TOKEN"), "csrf-sdk");
        return Response.json(token);
    });
    assert.deepEqual(await refresh(), token);
});

test("생성 응답 검증기는 숫자·nullable·enum 계약을 실제로 확인", async (t) => {
    setAccessToken(token);
    let response: unknown = user;
    t.mock.method(globalThis, "fetch", async () => Response.json(response));
    assert.deepEqual(await me(), user);
    for (response of [
        { ...user, userId: "1" },
        { ...user, userId: 1.5 },
        { ...user, userId: Number.MAX_SAFE_INTEGER + 1 },
        { ...user, role: "OTHER" },
        { nickname: "missing" },
    ]) {
        await assert.rejects(
            me(),
            (error: unknown) => error instanceof ApiError && error.kind === "invalid-response",
        );
    }
});

test("생성 함수도 토큰 없이 보호 요청을 보내지 않음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("전송되면 안 됨");
    });
    await assert.rejects(
        me(),
        (error: unknown) => error instanceof ApiError && error.kind === "authentication-required",
    );
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("SDK 비동기 준비 중 인증이 바뀌면 새 계정으로 이전 요청을 전송하지 않음", async (t) => {
    setAccessToken(token);
    const fetchMock = t.mock.method(globalThis, "fetch", async () => Response.json(user));
    const pending = me();
    setAccessToken({ ...token, accessToken: "new-token" });
    await assert.rejects(
        pending,
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
    assert.equal(getAccessToken(), "new-token");
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("SDK도 HTML·잘못된 JSON과 실제 네트워크 장애를 구분", async (t) => {
    setAccessToken(token);
    let mode = "html";
    t.mock.method(globalThis, "fetch", async () => {
        if (mode === "offline") throw new TypeError("offline");
        return new Response(mode === "html" ? "<html/>" : "{", {
            headers: { "Content-Type": mode === "html" ? "text/html" : "application/json" },
        });
    });
    for (mode of ["html", "json", "offline"]) {
        await assert.rejects(
            me(),
            (error: unknown) =>
                error instanceof ApiError &&
                error.kind === (mode === "offline" ? "network" : "invalid-response"),
        );
    }
});

test("생성 응답 검증 중 계정이 바뀌어도 이전 회원 정보를 반환하지 않음", async (t) => {
    setAccessToken(token);
    let release: () => void;
    let started: () => void;
    const ready = new Promise<void>((resolve) => {
        started = resolve;
    });
    const gate = new Promise<void>((resolve) => {
        release = resolve;
    });
    t.mock.method(globalThis, "fetch", async () => Response.json(user));
    const pending = me({
        responseValidator: async () => {
            started();
            await gate;
        },
    });
    await ready;
    clearAccessToken();
    release!();
    await assert.rejects(
        pending,
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
});

test("이미 취소한 SDK 로그인 요청은 전송하지 않고 취소로 분류", async (t) => {
    const controller = new AbortController();
    controller.abort();
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("전송되면 안 됨");
    });
    await assert.rejects(
        login({
            body: { email: "user@example.test", password: "password" },
            signal: controller.signal,
        }),
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("객체 응답 API의 빈 200·204·205를 정상 DTO로 전달하지 않음", async (t) => {
    setAccessToken(token);
    let response: Response;
    t.mock.method(globalThis, "fetch", async () => response);
    for (response of [
        new Response(null, {
            headers: { "Content-Type": "application/json", "Content-Length": "0" },
        }),
        new Response("", { headers: { "Content-Type": "application/json" } }),
        new Response(null, { status: 204 }),
        new Response(null, { status: 205 }),
    ]) {
        await assert.rejects(
            getCurrentUser(),
            (error: unknown) => error instanceof ApiError && error.kind === "invalid-response",
        );
    }
});

test("본문 없는 로그아웃의 204는 생성 SDK에서도 undefined로 반환", async (t) => {
    t.mock.method(globalThis, "fetch", async () => new Response(null, { status: 204 }));
    assert.equal(await logout(), undefined);
});

test("SDK 응답 검증 대기 중 취소하면 완료를 기다리거나 회원 정보를 반환하지 않음", async (t) => {
    setAccessToken(token);
    let release!: () => void;
    let started!: () => void;
    const ready = new Promise<void>((resolve) => {
        started = resolve;
    });
    const gate = new Promise<void>((resolve) => {
        release = resolve;
    });
    const controller = new AbortController();
    t.mock.method(globalThis, "fetch", async () => Response.json(user));
    const pending = me({
        signal: controller.signal,
        responseValidator: async () => {
            started();
            await gate;
        },
    });
    const outcome = pending.then(
        () => "fulfilled",
        (error: unknown) => (error instanceof ApiError ? error.kind : "unexpected"),
    );
    await ready;
    controller.abort();
    try {
        assert.equal(
            await Promise.race([
                outcome,
                new Promise<string>((resolve) => setImmediate(() => resolve("pending"))),
            ]),
            "aborted",
        );
    } finally {
        release();
        await outcome;
    }
    assert.equal(await outcome, "aborted");
    assert.equal(getAccessToken(), "sdk-token");
});

test("SDK의 JSON 파싱·응답 검증 오류에도 실제 HTTP 상태를 보존", async (t) => {
    setAccessToken(token);
    let body = "{";
    t.mock.method(
        globalThis,
        "fetch",
        async () => new Response(body, { headers: { "Content-Type": "application/json" } }),
    );
    for (body of ["{", JSON.stringify({ ...user, role: "INVALID" })]) {
        await assert.rejects(me(), (error: unknown) => {
            assert(error instanceof ApiError);
            assert.equal(error.kind, "invalid-response");
            assert.equal(error.status, 200);
            return true;
        });
    }
});
