/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { ensureCsrfToken, readCsrfToken } from "../src/api/csrf.ts";
import { withRequestPolicy } from "../src/api/http.ts";
import { sendRequest, type RequestOptions } from "../src/api/transport.ts";

function request(
    path: `/api/${string}`,
    options: RequestOptions & { auth?: "none" | "required" } = {},
): Promise<unknown> {
    return withRequestPolicy(path, options, sendRequest);
}

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const browserDocument = { cookie: "" };

function createGate(): { promise: Promise<void>; resolve: () => void } {
    let release: () => void;
    const promise = new Promise<void>((resolve) => {
        release = resolve;
    });
    return { promise, resolve: () => release() };
}

beforeEach(() => {
    browserDocument.cookie = "";
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: browserDocument,
    });
});

afterEach(() => {
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("쿠키 이름을 정확히 구분하고 빈 값·중복 쿠키는 사용하지 않음", () => {
    browserDocument.cookie = "other-XSRF-TOKEN=wrong; XSRF-TOKEN=raw=token; theme=dark";
    assert.equal(readCsrfToken(), "raw=token");
    for (const cookies of ["", "XSRF-TOKEN=", "XSRF-TOKEN=a; XSRF-TOKEN=b"]) {
        browserDocument.cookie = cookies;
        assert.equal(readCsrfToken(), undefined);
    }
});

test("기존 쿠키를 재사용하고 다음 호출에서 바뀐 값을 읽음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("쿠키가 있으므로 준비 요청이 없어야 함");
    });
    browserDocument.cookie = "XSRF-TOKEN=first";
    assert.equal(await ensureCsrfToken(), "first");
    browserDocument.cookie = "XSRF-TOKEN=second";
    assert.equal(await ensureCsrfToken(), "second");
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("동시 변경 요청은 쿠키 준비 GET 하나를 기다린 뒤 각각 헤더를 전달", async (t) => {
    const gate = createGate();
    let bootstraps = 0;
    let mutations = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(options?.credentials, "same-origin");
        if (path === "/api/auth/csrf") {
            bootstraps++;
            assert.equal(options?.cache, "no-store");
            await gate.promise;
            browserDocument.cookie = "XSRF-TOKEN=prepared";
        } else {
            mutations++;
            assert.equal(new Headers(options?.headers).get("X-XSRF-TOKEN"), "prepared");
        }
        return new Response(null, { status: 204 });
    });
    const pending = Promise.all([
        request("/api/auth/login", { method: "POST" }),
        request("/api/auth/signup", { method: "POST" }),
        request("/api/auth/email-verifications", { method: "POST" }),
    ]);
    assert.equal(bootstraps, 1);
    assert.equal(mutations, 0);
    gate.resolve();
    await pending;
    assert.equal(bootstraps, 1);
    assert.equal(mutations, 3);
});

test("쿠키 준비 실패를 함께 전달하고 다음 명시적 호출에서 다시 준비 가능", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new TypeError("offline");
    });
    const results = await Promise.allSettled([ensureCsrfToken(), ensureCsrfToken()]);
    for (const result of results) {
        assert.equal(result.status, "rejected");
        if (result.status === "rejected") {
            assert(result.reason instanceof ApiError);
            assert.equal(result.reason.kind, "network");
        }
    }
    assert.equal(fetchMock.mock.callCount(), 1);
    fetchMock.mock.mockImplementation(async () => {
        browserDocument.cookie = "XSRF-TOKEN=recovered";
        return new Response(null, { status: 204 });
    });
    assert.equal(await ensureCsrfToken(), "recovered");
    assert.equal(fetchMock.mock.callCount(), 2);
});

test("204 이후에도 쿠키가 없으면 원래 변경 요청을 보내지 않음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async (path: unknown) => {
        assert.equal(path, "/api/auth/csrf");
        return new Response(null, { status: 204 });
    });
    await assert.rejects(request("/api/auth/login", { method: "POST" }), (error: unknown) => {
        assert(error instanceof ApiError);
        assert.equal(error.kind, "invalid-response");
        return true;
    });
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("인증 변경 요청의 오래된 CSRF 헤더를 교체하고 다른 헤더·본문은 유지", async (t) => {
    browserDocument.cookie = "XSRF-TOKEN=current";
    const originalHeaders = new Headers({ "X-XSRF-TOKEN": "old", "X-Test": "keep" });
    t.mock.method(globalThis, "fetch", async (_path: unknown, options?: RequestInit) => {
        const headers = new Headers(options?.headers);
        assert.equal(headers.get("X-XSRF-TOKEN"), "current");
        assert.equal(headers.get("X-Test"), "keep");
        assert.equal(options?.body, "{}");
        return new Response(null, { status: 204 });
    });
    for (const method of ["POST", "PUT", "PATCH", "DELETE"]) {
        await request("/api/auth/account?source=test", {
            method,
            headers: originalHeaders,
            body: "{}",
        });
    }
    assert.equal(originalHeaders.get("X-XSRF-TOKEN"), "old");
});

test("조회 요청과 인증 경로 밖의 변경 요청에는 CSRF 준비·헤더를 추가하지 않음", async (t) => {
    const fetchMock = t.mock.method(
        globalThis,
        "fetch",
        async (_path: unknown, options?: RequestInit) => {
            assert.equal(new Headers(options?.headers).get("X-XSRF-TOKEN"), null);
            return new Response(null, { status: 204 });
        },
    );
    await request("/api/auth/csrf");
    await request("/api/auth/csrf", { method: "HEAD" });
    await request("/api/rooms", { method: "POST" });
    await request("/api/auth-other", { method: "POST" });
    assert.equal(fetchMock.mock.callCount(), 4);
});

test("미리 취소한 요청은 CSRF 준비 요청도 보내지 않음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("취소된 요청은 전송하지 않아야 함");
    });
    const controller = new AbortController();
    controller.abort();
    await assert.rejects(
        request("/api/auth/login", { method: "POST", signal: controller.signal }),
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("한 호출을 취소해도 다른 호출의 공유 쿠키 준비는 완료됨", async (t) => {
    const gate = createGate();
    let mutations = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        if (path === "/api/auth/csrf") {
            await gate.promise;
            browserDocument.cookie = "XSRF-TOKEN=prepared";
        } else {
            options?.signal?.throwIfAborted();
            mutations++;
        }
        return new Response(null, { status: 204 });
    });
    const controller = new AbortController();
    const canceled = request("/api/auth/login", { method: "POST", signal: controller.signal });
    const other = request("/api/auth/signup", { method: "POST" });
    controller.abort();
    gate.resolve();
    await assert.rejects(
        canceled,
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
    await other;
    assert.equal(mutations, 1);
});

test("CSRF 거절이나 전송 실패 후 변경 요청을 자동 반복하지 않음", async (t) => {
    browserDocument.cookie = "XSRF-TOKEN=current";
    let offline = false;
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        if (offline) throw new TypeError("response lost");
        return Response.json({ code: "INVALID_CSRF_TOKEN" }, { status: 403 });
    });
    await assert.rejects(request("/api/auth/login", { method: "POST" }), (error: unknown) => {
        assert(error instanceof ApiError);
        assert.equal(error.problem?.code, "INVALID_CSRF_TOKEN");
        return true;
    });
    assert.equal(fetchMock.mock.callCount(), 1);
    offline = true;
    await assert.rejects(request("/api/auth/login", { method: "POST" }));
    assert.equal(fetchMock.mock.callCount(), 2);
});

test("CSRF 준비가 끝나기 전 취소한 호출은 즉시 종료하고 다른 호출은 계속 진행", async (t) => {
    const gate = createGate();
    let mutations = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/csrf") {
            await gate.promise;
            browserDocument.cookie = "XSRF-TOKEN=prepared";
        } else mutations++;
        return new Response(null, { status: 204 });
    });
    const controller = new AbortController();
    const canceled = request("/api/auth/login", { method: "POST", signal: controller.signal });
    const other = request("/api/auth/signup", { method: "POST" });
    const outcome = canceled.then(
        () => "fulfilled",
        (error: unknown) => (error instanceof ApiError ? error.kind : "unexpected"),
    );
    controller.abort();
    try {
        assert.equal(
            await Promise.race([
                outcome,
                new Promise<string>((resolve) => setImmediate(() => resolve("pending"))),
            ]),
            "aborted",
        );
        assert.equal(mutations, 0);
    } finally {
        gate.resolve();
        await Promise.allSettled([canceled, other]);
    }
    assert.equal(mutations, 1);
});
