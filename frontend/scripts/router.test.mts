/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { api } from "../src/api/api.ts";
import { ApiError } from "../src/api/apiError.ts";
import { clearAccessToken, getAccessToken, setAccessToken } from "../src/api/auth/tokenStore.ts";
import { apiRoutes } from "../src/api/generated/schema.ts";
import { createApiRouter } from "../src/api/router.ts";
import { validateCurrentUser } from "../src/api/users/userResponse.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const token = { accessToken: "router-token", tokenType: "Bearer", expiresIn: 900 } as const;
const user = {
    userId: 1,
    email: null,
    nickname: "라우터",
    profileImageUrl: null,
    gender: null,
    birthYear: null,
    role: "USER",
};
const invalidResponse = (error: unknown): boolean =>
    error instanceof ApiError && error.kind === "invalid-response";

beforeEach(() => {
    clearAccessToken();
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=router-csrf" },
    });
});
afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("별도 호출 함수 없이 operationId와 입력만으로 방 생성 요청을 구성", async (t) => {
    setAccessToken(token);
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/rooms");
        assert.equal(options?.method, "POST");
        assert.equal(new Headers(options?.headers).get("Authorization"), "Bearer router-token");
        assert.deepEqual(JSON.parse(String(options?.body)), { name: "여행" });
        return Response.json({ roomId: 3 }, { status: 201 });
    });
    assert.deepEqual(await api.call("create", { body: { name: "여행" } }), { roomId: 3 });
});

test("명세의 Bearer·CSRF 조합과 PATCH 메서드를 호출 옵션 없이 적용", async (t) => {
    setAccessToken(token);
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/auth/password");
        assert.equal(options?.method, "PATCH");
        assert.equal(new Headers(options?.headers).get("Authorization"), "Bearer router-token");
        assert.equal(new Headers(options?.headers).get("X-XSRF-TOKEN"), "router-csrf");
        return new Response(null, { status: 204 });
    });
    assert.equal(
        await api.call("change", { body: { accountProof: "proof", newPassword: "password" } }),
        undefined,
    );
});

test("프로필 PATCH는 Bearer로 한 번 전송하고 생략·null을 그대로 전달", async (t) => {
    setAccessToken(token);
    const body = { nickname: "바뀐 닉네임", gender: null, birthYear: 1990 };
    const updated = { ...user, ...body };
    const fetchMock = t.mock.method(
        globalThis,
        "fetch",
        async (path: unknown, options?: RequestInit) => {
            assert.equal(path, "/api/users/me");
            assert.equal(options?.method, "PATCH");
            const headers = new Headers(options?.headers);
            assert.equal(headers.get("Authorization"), "Bearer router-token");
            assert.equal(headers.has("X-XSRF-TOKEN"), false);
            assert.deepEqual(JSON.parse(String(options?.body)), body);
            return Response.json(updated);
        },
    );
    assert.deepEqual(await api.call("updateProfile", { body }), updated);
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("프로필 변경의 401은 자동 갱신·재전송하지 않음", async (t) => {
    setAccessToken(token);
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        Response.json({ status: 401, code: "UNAUTHENTICATED" }, { status: 401 }),
    );
    await assert.rejects(
        api.call("updateProfile", { body: { nickname: "변경" } }),
        (error: unknown) => error instanceof ApiError && error.status === 401,
    );
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("프로필 조회·변경 응답의 선택 정보는 null을 허용하고 누락·잘못된 타입은 거절", async (t) => {
    setAccessToken(token);
    assert.deepEqual(validateCurrentUser(user, 200), user);
    const configured = { ...user, gender: "FEMALE", birthYear: 1995 };
    assert.deepEqual(validateCurrentUser(configured, 200), configured);
    for (const field of ["gender", "birthYear"]) {
        const missing: Record<string, unknown> = { ...user };
        Reflect.deleteProperty(missing, field);
        assert.throws(() => validateCurrentUser(missing, 200), invalidResponse);
    }
    for (const value of [
        { ...user, gender: "OTHER" },
        { ...user, gender: 0 },
        { ...user, birthYear: "1995" },
        { ...user, birthYear: 1995.5 },
        { ...user, birthYear: 0 },
    ]) {
        assert.throws(() => validateCurrentUser(value, 200), invalidResponse);
    }
    t.mock.method(globalThis, "fetch", async () => Response.json({ ...user, gender: "OTHER" }));
    await assert.rejects(api.call("updateProfile", { body: {} }), invalidResponse);
    await assert.rejects(api.call("me"), invalidResponse);
});

test("엔드포인트 확장은 공통 요청 안에서 전처리·응답 검증·후처리를 한 번 실행", async (t) => {
    setAccessToken(token);
    const order: string[] = [];
    const router = createApiRouter(apiRoutes, {
        extensions: {
            me: {
                parseResponse: (data, status) => {
                    order.push("parse");
                    return validateCurrentUser(data, status);
                },
                wrap: async (next, context) => {
                    assert.equal(context.operationId, "me");
                    order.push("before");
                    const data = await next();
                    order.push("after");
                    return data;
                },
            },
        },
    });
    t.mock.method(globalThis, "fetch", async () => {
        order.push("fetch");
        return Response.json(user);
    });
    assert.deepEqual(await router.call("me"), user);
    assert.deepEqual(order, ["before", "fetch", "parse", "after"]);
});

test("알 수 없는 operationId·프로토타입 키는 전송 전에 거절", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => assert.fail("전송 금지"));
    for (const id of ["missing", "__proto__", "toString"]) {
        await assert.rejects(Reflect.apply(api.call, api, [id]), invalidResponse);
    }
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("런타임의 추가 옵션도 인증·fetch·출처 정책을 바꿀 수 없음", async (t) => {
    setAccessToken(token);
    const replacement = t.mock.fn(async () => assert.fail("우회 금지"));
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/users/me");
        assert.equal(new Headers(options?.headers).get("Authorization"), "Bearer router-token");
        assert.equal(options?.credentials, "same-origin");
        return Response.json(user);
    });
    assert.deepEqual(
        await Reflect.apply(api.call, api, [
            "me",
            {
                auth: "none",
                fetch: replacement,
                baseUrl: "https://example.invalid",
                credentials: "omit",
            },
        ]),
        user,
    );
    assert.equal(replacement.mock.callCount(), 0);
    assert.equal(getAccessToken(), "router-token");
});

test("명세와 다른 성공 상태는 HTTP 상태를 보존해 거절", async (t) => {
    setAccessToken(token);
    t.mock.method(globalThis, "fetch", async () => Response.json({ roomId: 3 }));
    await assert.rejects(api.call("create", { body: { name: "여행" } }), (error: unknown) => {
        assert(error instanceof ApiError);
        assert.equal(error.kind, "invalid-response");
        assert.equal(error.status, 200);
        return true;
    });
});

test("본문 없는 성공 계약은 빈 200도 허용하고 예상하지 않은 본문은 거절", async (t) => {
    const router = createApiRouter({
        ...apiRoutes,
        logout: { ...apiRoutes.logout, responses: { "200": false } },
    });
    let body: string | null = null;
    t.mock.method(globalThis, "fetch", async () => new Response(body));
    assert.equal(await router.call("logout"), undefined);
    body = "{}";
    await assert.rejects(router.call("logout"), invalidResponse);
});
