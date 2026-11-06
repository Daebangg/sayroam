/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { loginApi } from "../src/api/auth/authApi.ts";
import { signIn } from "../src/api/auth/authSession.ts";
import {
    clearAccessToken,
    getAccessToken,
    getAccessTokenSnapshot,
    getAuthRevision,
    setAccessToken,
} from "../src/api/auth/tokenStore.ts";
import { request } from "../src/api/http.ts";
import { getCurrentUser } from "../src/api/users/userApi.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const input = { email: "user@example.test", password: " password with spaces " };
const issued = { accessToken: "access-a", tokenType: "Bearer", expiresIn: 900 } as const;
const user = { userId: 1, email: null, nickname: "여행자", profileImageUrl: null, role: "USER" };

function createGate(): { promise: Promise<void>; resolve: () => void } {
    let release: () => void;
    const promise = new Promise<void>((resolve) => {
        release = resolve;
    });
    return { promise, resolve: () => release() };
}

function hasKind(kind: string): (error: unknown) => boolean {
    return (error: unknown) => {
        assert(error instanceof ApiError);
        assert.equal(error.kind, kind);
        return true;
    };
}

beforeEach(() => {
    clearAccessToken();
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=csrf-test" },
    });
});

afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("토큰과 참고 만료 시각을 메모리에 저장하고 정리할 때 인증 버전 변경", () => {
    const revision = getAuthRevision();
    const now = Date.now();
    setAccessToken(issued);
    assert.equal(getAccessToken(), issued.accessToken);
    const snapshot = getAccessTokenSnapshot();
    assert(snapshot);
    assert.equal(Object.isFrozen(snapshot), true);
    assert(snapshot.expiresAt >= now + 900_000);
    assert(snapshot.revision > revision);
    clearAccessToken();
    assert.equal(getAccessToken(), undefined);
    assert(getAuthRevision() > snapshot.revision);
});

test("토큰 없는 보호 요청은 HTTP 응답으로 위장하지 않고 전송 전에 구분", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("인증 없이 보호 요청을 보내면 안 됨");
    });
    await assert.rejects(getCurrentUser(), hasKind("authentication-required"));
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("보호 요청은 현재 토큰을 사용하고 인증 전용 옵션을 fetch에 전달하지 않음", async (t) => {
    setAccessToken(issued);
    t.mock.method(globalThis, "fetch", async (_path: unknown, options?: RequestInit) => {
        const headers = new Headers(options?.headers);
        assert.equal(headers.get("Authorization"), "Bearer access-a");
        assert.equal(headers.get("X-Test"), "keep");
        assert.equal(options && "auth" in options, false);
        return Response.json(user);
    });
    assert.deepEqual(
        await request("/api/users/me", { auth: "required", headers: { "X-Test": "keep" } }),
        user,
    );
});

test("공개 요청은 저장된 토큰이나 호출자가 넣은 Bearer 헤더를 전송하지 않음", async (t) => {
    setAccessToken(issued);
    t.mock.method(globalThis, "fetch", async (_path: unknown, options?: RequestInit) => {
        assert.equal(new Headers(options?.headers).get("Authorization"), null);
        return new Response(null, { status: 204 });
    });
    await request("/api/auth/csrf", { headers: { Authorization: "Bearer old" } });
});

test("인증 경로의 보호 변경 요청에는 Bearer와 CSRF 헤더를 함께 전달", async (t) => {
    setAccessToken(issued);
    t.mock.method(globalThis, "fetch", async (_path: unknown, options?: RequestInit) => {
        const headers = new Headers(options?.headers);
        assert.equal(headers.get("Authorization"), "Bearer access-a");
        assert.equal(headers.get("X-XSRF-TOKEN"), "csrf-test");
        return new Response(null, { status: 204 });
    });
    await request("/api/auth/password", { auth: "required", method: "POST" });
});

test("로그인은 비밀번호를 변형하지 않고 전달한 뒤 토큰을 저장하고 회원을 조회", async (t) => {
    setAccessToken({ ...issued, accessToken: "previous" });
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        paths.push(path);
        const headers = new Headers(options?.headers);
        if (path === "/api/auth/login") {
            assert.equal(getAccessToken(), undefined);
            assert.equal(headers.get("Authorization"), null);
            assert.equal(headers.get("X-XSRF-TOKEN"), "csrf-test");
            assert.deepEqual(JSON.parse(String(options?.body)), input);
            return Response.json(issued);
        }
        assert.equal(headers.get("Authorization"), "Bearer access-a");
        return Response.json(user);
    });
    assert.deepEqual(await signIn(input), user);
    assert.deepEqual(paths, ["/api/auth/login", "/api/users/me"]);
    assert.equal(getAccessToken(), "access-a");
});

test("로그인 정보 불일치는 원래 코드를 보존하고 로그인 POST를 반복하지 않음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        Response.json({ code: "INVALID_CREDENTIALS" }, { status: 401 }),
    );
    await assert.rejects(signIn(input), (error: unknown) => {
        assert(error instanceof ApiError);
        assert.equal(error.status, 401);
        assert.equal(error.problem?.code, "INVALID_CREDENTIALS");
        return true;
    });
    assert.equal(getAccessToken(), undefined);
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("잘못된 로그인 응답은 저장하거나 회원 조회에 사용하지 않음", async (t) => {
    let body: unknown;
    const fetchMock = t.mock.method(globalThis, "fetch", async () => Response.json(body));
    for (body of [
        null,
        {},
        { ...issued, accessToken: "" },
        { ...issued, accessToken: "a\nb" },
        { ...issued, tokenType: "Basic" },
        { ...issued, expiresIn: 0 },
    ]) {
        await assert.rejects(signIn(input), hasKind("invalid-response"));
        assert.equal(getAccessToken(), undefined);
    }
    assert.equal(fetchMock.mock.callCount(), 6);
});

test("생성 계약의 nullable 회원 정보는 허용하고 잘못된 역할·필수 필드는 거절", async (t) => {
    setAccessToken(issued);
    let body: unknown = user;
    t.mock.method(globalThis, "fetch", async () => Response.json(body));
    assert.deepEqual(await getCurrentUser(), user);
    for (body of [
        { ...user, role: "UNKNOWN" },
        { ...user, email: 1 },
        { ...user, userId: 0 },
        { userId: 1 },
    ]) {
        await assert.rejects(getCurrentUser(), hasKind("invalid-response"));
    }
});

test("현재 토큰의 401 뒤 갱신도 거절되면 메모리를 정리하고 반복하지 않음", async (t) => {
    setAccessToken(issued);
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        Response.json({ code: "UNAUTHENTICATED" }, { status: 401 }),
    );
    await assert.rejects(getCurrentUser(), hasKind("http"));
    assert.equal(getAccessToken(), undefined);
    assert.equal(fetchMock.mock.callCount(), 2);
});

test("이전 토큰의 늦은 401은 새 토큰을 지우지 않음", async (t) => {
    setAccessToken(issued);
    const gate = createGate();
    t.mock.method(globalThis, "fetch", async () => {
        await gate.promise;
        return Response.json({ code: "UNAUTHENTICATED" }, { status: 401 });
    });
    const pending = getCurrentUser();
    setAccessToken({ ...issued, accessToken: "access-b" });
    gate.resolve();
    await assert.rejects(pending, hasKind("aborted"));
    assert.equal(getAccessToken(), "access-b");
});

test("권한 부족·서버 장애·네트워크 실패를 토큰 만료로 처리하지 않음", async (t) => {
    setAccessToken(issued);
    let status = 403;
    t.mock.method(globalThis, "fetch", async () => {
        if (status === 0) throw new TypeError("offline");
        return Response.json({ code: "UNAVAILABLE" }, { status });
    });
    for (status of [403, 503, 0]) {
        await assert.rejects(getCurrentUser());
        assert.equal(getAccessToken(), "access-a");
    }
});

test("진행 중 로그인에 대한 중복 호출은 두 번째 POST를 만들지 않음", async (t) => {
    const gate = createGate();
    const fetchMock = t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/login") {
            await gate.promise;
            return Response.json(issued);
        }
        return Response.json(user);
    });
    const pending = signIn(input);
    await assert.rejects(signIn(input), hasKind("authentication-in-progress"));
    gate.resolve();
    assert.deepEqual(await pending, user);
    assert.equal(fetchMock.mock.callCount(), 2);
});

test("로그인 응답 전에 인증을 정리하면 늦은 성공이 토큰을 복원하지 않음", async (t) => {
    const gate = createGate();
    const started = createGate();
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        started.resolve();
        await gate.promise;
        return Response.json(issued);
    });
    const pending = signIn(input);
    await started.promise;
    clearAccessToken();
    gate.resolve();
    await assert.rejects(pending, hasKind("aborted"));
    assert.equal(getAccessToken(), undefined);
    assert.equal(fetchMock.mock.callCount(), 1);
});

test("토큰 정리 후 이전 회원 조회의 성공 응답을 전달하지 않음", async (t) => {
    setAccessToken(issued);
    const gate = createGate();
    t.mock.method(globalThis, "fetch", async () => {
        await gate.promise;
        return Response.json(user);
    });
    const pending = getCurrentUser();
    clearAccessToken();
    gate.resolve();
    await assert.rejects(pending, hasKind("aborted"));
});

test("로그인 후 회원 조회가 일시 실패하면 발급된 토큰은 유지하고 로그인 재전송 없음", async (t) => {
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        paths.push(path);
        if (path === "/api/auth/login") return Response.json(issued);
        throw new TypeError("profile unavailable");
    });
    await assert.rejects(signIn(input), hasKind("network"));
    assert.equal(getAccessToken(), "access-a");
    assert.deepEqual(paths, ["/api/auth/login", "/api/users/me"]);
});

test("취소된 로그인은 기존 인증을 지우거나 요청을 전송하지 않음", async (t) => {
    setAccessToken(issued);
    const controller = new AbortController();
    controller.abort();
    const fetchMock = t.mock.method(globalThis, "fetch", async () => {
        throw new Error("취소된 로그인은 전송하지 않아야 함");
    });
    await assert.rejects(signIn(input, { signal: controller.signal }), hasKind("aborted"));
    assert.equal(getAccessToken(), "access-a");
    assert.equal(fetchMock.mock.callCount(), 0);
});

test("통신용 로그인 함수 자체는 세션 상태를 변경하지 않음", async (t) => {
    t.mock.method(globalThis, "fetch", async () => Response.json(issued));
    assert.deepEqual(await loginApi(input), issued);
    assert.equal(getAccessToken(), undefined);
});
