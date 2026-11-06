/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { refreshApi } from "../src/api/auth/authApi.ts";
import { restoreSession, signOut } from "../src/api/auth/authSession.ts";
import { refreshAccessToken } from "../src/api/auth/refreshSession.ts";
import {
    clearAccessToken,
    getAccessToken,
    getAuthRevision,
    setAccessToken,
} from "../src/api/auth/tokenStore.ts";
import { api } from "../src/api/api.ts";
import { getCurrentUser as me } from "../src/api/users/userApi.ts";

function create(): Promise<unknown> {
    return api.call("create", { body: { name: "여행" } });
}

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const token = { accessToken: "old-token", tokenType: "Bearer", expiresIn: 900 } as const;
const next = { ...token, accessToken: "new-token" };
const user = { userId: 1, email: null, nickname: "갱신", profileImageUrl: null, role: "USER" };

function gate(): { promise: Promise<void>; resolve: () => void } {
    let resolve!: () => void;
    const promise = new Promise<void>((release) => {
        resolve = release;
    });
    return { promise, resolve };
}

function hasKind(kind: string): (error: unknown) => boolean {
    return (error) => error instanceof ApiError && error.kind === kind;
}

beforeEach(() => {
    clearAccessToken();
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=csrf-refresh" },
    });
});
afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("동시 401은 갱신 하나를 공유하고 각 조회를 새 토큰으로 한 번 재전송", async (t) => {
    setAccessToken(token);
    const revision = getAuthRevision();
    const started = gate();
    const release = gate();
    let refreshes = 0;
    let oldRequests = 0;
    let newRequests = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        const headers = new Headers(options?.headers);
        if (path === "/api/auth/refresh") {
            refreshes++;
            assert.equal(headers.get("Authorization"), null);
            assert.equal(headers.get("X-XSRF-TOKEN"), "csrf-refresh");
            assert.equal(options?.credentials, "same-origin");
            assert.equal(headers.get("Cookie"), null);
            started.resolve();
            await release.promise;
            return Response.json(next);
        }
        if (headers.get("Authorization") === "Bearer old-token") {
            oldRequests++;
            return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
        }
        assert.equal(headers.get("Authorization"), "Bearer new-token");
        newRequests++;
        return Response.json(user);
    });
    const requests = [me(), me(), me()];
    await started.promise;
    release.resolve();
    assert.deepEqual(await Promise.all(requests), [user, user, user]);
    assert.equal(refreshes, 1);
    assert.equal(oldRequests, 3);
    assert.equal(newRequests, 3);
    assert.equal(getAuthRevision(), revision);
    assert.equal(getAccessToken(), "new-token");
});

test("늦게 도착한 이전 토큰의 401은 갱신을 다시 실행하지 않음", async (t) => {
    setAccessToken(token);
    const delayed = gate();
    let oldRequests = 0;
    let refreshes = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        if (path === "/api/auth/refresh") {
            refreshes++;
            return Response.json(next);
        }
        if (new Headers(options?.headers).get("Authorization") === "Bearer old-token") {
            oldRequests++;
            if (oldRequests === 2) await delayed.promise;
            return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
        }
        return Response.json(user);
    });
    const first = me();
    const late = me();
    await first;
    delayed.resolve();
    assert.deepEqual(await late, user);
    assert.equal(refreshes, 1);
});

test("한 대기자의 취소가 공유 갱신을 중단하거나 다른 조회를 취소하지 않음", async (t) => {
    setAccessToken(token);
    const started = gate();
    const release = gate();
    let retries = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        if (path === "/api/auth/refresh") {
            assert.equal(options?.signal?.aborted, false);
            started.resolve();
            await release.promise;
            return Response.json(next);
        }
        if (new Headers(options?.headers).get("Authorization") === "Bearer old-token") {
            return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
        }
        retries++;
        return Response.json(user);
    });
    const controller = new AbortController();
    const canceled = me({ signal: controller.signal });
    const other = me();
    await started.promise;
    controller.abort();
    release.resolve();
    await assert.rejects(canceled, hasKind("aborted"));
    assert.deepEqual(await other, user);
    assert.equal(retries, 1);
    assert.equal(getAccessToken(), "new-token");
});

test("갱신 401·403·503·네트워크·잘못된 응답은 상태를 정리하고 재전송하지 않음", async (t) => {
    let mode: number | string = 401;
    let refreshes = 0;
    let reads = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path !== "/api/auth/refresh") {
            reads++;
            return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
        }
        refreshes++;
        if (mode === "network") throw new TypeError("response lost");
        if (mode === "empty")
            return new Response(null, {
                headers: { "Content-Type": "application/json", "Content-Length": "0" },
            });
        if (mode === "invalid") return Response.json({ ...next, accessToken: "" });
        return Response.json({ code: "REFRESH_FAILED" }, { status: Number(mode) });
    });
    for (mode of [401, 403, 503, "network", "empty", "invalid"]) {
        setAccessToken(token);
        await assert.rejects(
            me(),
            hasKind(
                typeof mode === "number"
                    ? "http"
                    : mode === "network"
                      ? "network"
                      : "invalid-response",
            ),
        );
        assert.equal(getAccessToken(), undefined);
        // 뒤따르는 보호 요청이 실패한 Refresh를 자동 반복하지 않는다.
        await assert.rejects(me(), hasKind("authentication-required"));
    }
    assert.equal(refreshes, 6);
    assert.equal(reads, 6);
});

test("갱신 후 조회도 401이면 두 번째 갱신 없이 로그인 상태 정리", async (t) => {
    setAccessToken(token);
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        paths.push(path);
        return path === "/api/auth/refresh"
            ? Response.json(next)
            : Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
    });
    await assert.rejects(me(), hasKind("http"));
    assert.deepEqual(paths, ["/api/users/me", "/api/auth/refresh", "/api/users/me"]);
    assert.equal(getAccessToken(), undefined);
});

test("변경 요청의 401은 자동 갱신·재전송 없이 전달", async (t) => {
    setAccessToken(token);
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        paths.push(path);
        return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
    });
    await assert.rejects(create(), hasKind("http"));
    assert.deepEqual(paths, ["/api/rooms"]);
    assert.equal(getAccessToken(), undefined);
});

test("변경 요청의 응답 유실은 토큰을 유지하고 자동 반복하지 않음", async (t) => {
    setAccessToken(token);
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new TypeError("lost");
    });
    await assert.rejects(create(), hasKind("network"));
    assert.equal(fetchMock.mock.callCount(), 1);
    assert.equal(getAccessToken(), "old-token");
});

test("로그아웃 이후 늦은 갱신 성공은 토큰을 복원하지 않음", async (t) => {
    setAccessToken(token);
    const started = gate();
    const release = gate();
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/logout") return new Response(null, { status: 204 });
        started.resolve();
        await release.promise;
        return Response.json(next);
    });
    const refreshing = refreshAccessToken(refreshApi);
    await started.promise;
    await signOut();
    release.resolve();
    await assert.rejects(refreshing, hasKind("aborted"));
    assert.equal(getAccessToken(), undefined);
});

test("갱신 중 새 로그인의 토큰이 저장되면 이전 갱신 결과로 덮어쓰지 않음", async (t) => {
    setAccessToken(token);
    const started = gate();
    const release = gate();
    t.mock.method(globalThis, "fetch", async () => {
        started.resolve();
        await release.promise;
        return Response.json(next);
    });
    const refreshing = refreshAccessToken(refreshApi);
    await started.promise;
    setAccessToken({ ...token, accessToken: "other-account" });
    release.resolve();
    await assert.rejects(refreshing, hasKind("aborted"));
    assert.equal(getAccessToken(), "other-account");
});

test("메모리 없는 세션 복원은 갱신 하나를 공유하고 회원 정보를 조회", async (t) => {
    let refreshes = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/refresh") {
            refreshes++;
            return Response.json(next);
        }
        return Response.json(user);
    });
    assert.deepEqual(await Promise.all([restoreSession(), restoreSession()]), [user, user]);
    assert.deepEqual(await restoreSession(), user);
    assert.equal(refreshes, 1);
});

test("로그아웃은 먼저 메모리를 지우고 쿠키·CSRF로 한 번 요청하며 실패를 보존", async (t) => {
    setAccessToken(token);
    const fetchMock = t.mock.method(
        globalThis,
        "fetch",
        async (path: unknown, options?: RequestInit) => {
            assert.equal(path, "/api/auth/logout");
            assert.equal(getAccessToken(), undefined);
            assert.equal(new Headers(options?.headers).get("Authorization"), null);
            assert.equal(new Headers(options?.headers).get("X-XSRF-TOKEN"), "csrf-refresh");
            return Response.json({ code: "AUTHENTICATION_UNAVAILABLE" }, { status: 503 });
        },
    );
    await assert.rejects(signOut(), hasKind("http"));
    assert.equal(getAccessToken(), undefined);
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("갱신의 CSRF 준비가 실패하면 Refresh POST를 보내지 않고 상태 정리", async (t) => {
    setAccessToken(token);
    Object.defineProperty(globalThis, "document", { configurable: true, value: { cookie: "" } });
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        paths.push(path);
        return new Response(null, { status: 204 });
    });
    await assert.rejects(refreshAccessToken(refreshApi), hasKind("invalid-response"));
    assert.deepEqual(paths, ["/api/auth/csrf"]);
    assert.equal(getAccessToken(), undefined);
});

test("복원 대기 취소는 공유 갱신 완료를 기다리지 않고 다른 복원은 유지", async (t) => {
    const started = gate();
    const release = gate();
    let refreshes = 0;
    let reads = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        if (path === "/api/auth/refresh") {
            refreshes++;
            started.resolve();
            await release.promise;
            assert.equal(options?.signal?.aborted, false);
            return Response.json(next);
        }
        reads++;
        return Response.json(user);
    });
    const controller = new AbortController();
    const canceled = restoreSession({ signal: controller.signal });
    const other = restoreSession();
    const outcome = canceled.then(
        () => "fulfilled",
        (error: unknown) => (error instanceof ApiError ? error.kind : "unexpected"),
    );
    await started.promise;
    controller.abort();
    try {
        assert.equal(
            await Promise.race([
                outcome,
                new Promise<string>((resolve) => setImmediate(() => resolve("pending"))),
            ]),
            "aborted",
        );
        assert.equal(getAccessToken(), undefined);
    } finally {
        release.resolve();
        await Promise.allSettled([canceled, other]);
    }
    assert.deepEqual(await other, user);
    assert.equal(refreshes, 1);
    assert.equal(reads, 1);
    assert.equal(getAccessToken(), "new-token");
});
