/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import type { CurrentUserResponse } from "../src/api/users/userApi.ts";
import { ApiError } from "../src/api/apiError.ts";
import { clearAccessToken, getAccessToken } from "../src/api/auth/tokenStore.ts";
import { createAuthController } from "../src/hooks/api/auth/authController.ts";
import {
    isValidEmail,
    isValidPassword,
    isValidNewPassword,
    isValidNickname,
} from "../src/hooks/authValidation.ts";
import {
    validateIssued,
    validateResetProof,
    validateSignupProof,
} from "../src/api/auth/emailResponse.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const credentials = { email: "user@example.test", password: " password with spaces " };
const token = { accessToken: "access-a", tokenType: "Bearer", expiresIn: 900 };
const user: CurrentUserResponse = {
    userId: 1,
    email: "user@example.test",
    nickname: "여행자",
    profileImageUrl: null,
    gender: null,
    birthYear: null,
    role: "USER",
    createdAt: "2026-11-10T12:00:00Z",
    birthDate: null,
};

function problem(status: number, code: string): Response {
    return Response.json({ status, code, title: code, detail: code, instance: "/" }, { status });
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

test("새 방문자의 갱신 거절은 미로그인으로, 서버 장애는 확인 불가로 표시", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        problem(401, "REFRESH_TOKEN_INVALID"),
    );
    const session = createAuthController();
    assert.equal(await session.restore(), false);
    assert.deepEqual(session.getSnapshot(), { status: "anonymous", user: null, message: null });
    fetchMock.mock.mockImplementation(async () => problem(503, "AUTHENTICATION_UNAVAILABLE"));
    assert.equal(await session.restore(), false);
    assert.equal(session.getSnapshot().status, "unavailable");
    assert.equal(session.getSnapshot().message, "unavailable");
    session.resetDialog();
    assert.deepEqual(session.getSnapshot(), {
        status: "unavailable",
        user: null,
        message: "unavailable",
        failureStatus: 503,
    });
});

test("복원 요청을 공유하고 로그인 성공 후 회원 상태 하나를 반영", async (t) => {
    const calls: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        calls.push(path);
        return Response.json(path === "/api/users/me" ? user : token);
    });
    const session = createAuthController();
    const first = session.restore();
    assert.equal(session.restore(), first);
    assert.equal(await first, true);
    assert.deepEqual(calls, ["/api/auth/refresh", "/api/users/me"]);
    assert.deepEqual(session.getSnapshot().user, user);
});

test("없는 이메일과 잘못된 비밀번호를 같은 실패 메시지로 표시", async (t) => {
    t.mock.method(globalThis, "fetch", async () => problem(401, "INVALID_CREDENTIALS"));
    const session = createAuthController();
    assert.equal(await session.login(credentials), false);
    assert.deepEqual(session.getSnapshot(), {
        status: "anonymous",
        user: null,
        message: "credentials",
    });
});

test("로그인 실패의 남은 횟수는 서버 응답을 사용하고 다음 응답에 없으면 유지하지 않음", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        Response.json(
            { status: 401, code: "INVALID_CREDENTIALS", remainingAttempts: 4 },
            { status: 401 },
        ),
    );
    const session = createAuthController();
    assert.equal(await session.login(credentials), false);
    assert.equal(session.getSnapshot().remainingAttempts, 4);
    fetchMock.mock.mockImplementation(async () => problem(401, "INVALID_CREDENTIALS"));
    assert.equal(await session.login(credentials), false);
    assert.equal(session.getSnapshot().remainingAttempts, undefined);
});

test("로그인 제한의 서버 대기 시간을 표시하고 만료 전 POST를 반복하지 않음", async (t) => {
    const now = Date.now();
    const clock = t.mock.method(Date, "now", () => now);
    const fetchMock = t.mock.method(globalThis, "fetch", async () =>
        Response.json(
            { status: 429, code: "LOGIN_RATE_LIMITED", retryAfterSeconds: 300 },
            { status: 429, headers: { "Retry-After": "300" } },
        ),
    );
    const session = createAuthController();
    assert.equal(await session.login(credentials), false);
    assert.equal(session.getSnapshot().message, "limited");
    assert.equal(session.getSnapshot().retryAt, now + 300_000);
    assert.equal(await session.login(credentials), false);
    assert.equal(fetchMock.mock.calls.length, 1);
    clock.mock.mockImplementation(() => now + 301_000);
    await session.login(credentials);
    assert.equal(fetchMock.mock.calls.length, 2);
});

test("숨김 문자·제어 문자를 새 비밀번호와 닉네임에서 거절하고 기존 긴 비밀번호 로그인은 유지", () => {
    assert.equal(isValidNewPassword("Aa1!\u200baaaa"), false);
    assert.equal(isValidNickname("name\u200b"), false);
    assert.equal(isValidNickname("name\u0085"), false);
    assert.equal(isValidNickname(" 제주 여행자😀 "), true);
    assert.equal(isValidPassword("x".repeat(128)), true);
    assert.equal(isValidNewPassword("Aa1!" + "a".repeat(21)), false);
});

test("로그인했던 회원의 만료·갱신 거절은 새 방문자의 미로그인과 구분해 재로그인을 안내", async (t) => {
    let expired = false;
    const paths: unknown[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        paths.push(path);
        if (path === "/api/auth/login") return Response.json(token);
        if (expired) return problem(401, "REFRESH_TOKEN_INVALID");
        return Response.json(user);
    });
    const session = createAuthController();
    const disconnect = session.connect();
    try {
        assert.equal(await session.login(credentials), true);
        expired = true;
        const now = Date.now();
        t.mock.method(Date, "now", () => now + 901_000);
        assert.equal(await session.check(), false);
        assert.deepEqual(session.getSnapshot(), {
            status: "anonymous",
            user: null,
            message: "expired",
        });
        assert.equal(paths.filter((path) => path === "/api/auth/refresh").length, 1);
        assert.equal(paths.filter((path) => path === "/api/auth/login").length, 1);
    } finally {
        disconnect();
    }
});

test("회원 조회 실패 후 모달을 닫아도 토큰과 재시도 안내를 유지하고 로그인 POST를 반복하지 않음", async (t) => {
    let logins = 0;
    let reads = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/login") {
            logins++;
            return Response.json(token);
        }
        return ++reads === 1 ? problem(503, "INTERNAL_SERVER_ERROR") : Response.json(user);
    });
    const session = createAuthController();
    assert.equal(await session.login(credentials), false);
    assert.equal(session.getSnapshot().message, "profileUnavailable");
    assert.equal(getAccessToken(), "access-a");
    session.resetDialog();
    assert.deepEqual(session.getSnapshot(), {
        status: "unavailable",
        user: null,
        message: "profileUnavailable",
        failureStatus: 503,
    });
    assert.equal(getAccessToken(), "access-a");
    assert.equal(await session.restore(), true);
    assert.equal(session.getSnapshot().status, "authenticated");
    assert.equal(logins, 1);
    assert.equal(reads, 2);
});

test("로그아웃 실패 후 모달을 닫아도 재시도 안내를 유지하고 명시적 재시도로 완료", async (t) => {
    const fetchMock = t.mock.method(globalThis, "fetch", async (path: unknown) =>
        path === "/api/auth/logout"
            ? problem(503, "AUTHENTICATION_UNAVAILABLE")
            : Response.json(path === "/api/users/me" ? user : token),
    );
    const session = createAuthController();
    const disconnect = session.connect();
    try {
        await session.login(credentials);
        const logout = session.logout();
        assert.equal(session.getSnapshot().user, null);
        assert.equal(getAccessToken(), undefined);
        assert.equal(await logout, false);
        assert.equal(session.getSnapshot().message, "logoutUncertain");
        session.resetDialog();
        assert.deepEqual(session.getSnapshot(), {
            status: "unavailable",
            user: null,
            message: "logoutUncertain",
        });
        assert.equal(getAccessToken(), undefined);
        fetchMock.mock.mockImplementation(async () => new Response(null, { status: 204 }));
        assert.equal(await session.logout(), true);
        assert.deepEqual(session.getSnapshot(), { status: "anonymous", user: null, message: null });
    } finally {
        disconnect();
    }
});

test("다른 탭의 무효화·갱신 장애가 회원 메뉴와 보호 상태에 즉시 반영", async (t) => {
    t.mock.method(globalThis, "fetch", async (path: unknown) =>
        Response.json(path === "/api/users/me" ? user : token),
    );
    const session = createAuthController();
    const disconnect = session.connect();
    try {
        await session.login(credentials);
        clearAccessToken();
        assert.equal(session.getSnapshot().message, "expired");
        assert.equal(session.getSnapshot().user, null);
        await session.login(credentials);
        clearAccessToken(new ApiError("network"));
        assert.equal(session.getSnapshot().status, "unavailable");
        assert.equal(session.getSnapshot().message, "unavailable");
    } finally {
        disconnect();
    }
});

test("중복 로그인과 연결 해제 후 늦게 도착한 결과로 회원 상태가 되살아나지 않음", async (t) => {
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => {
        release = resolve;
    });
    let logins = 0;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/login") {
            logins++;
            await gate;
            return Response.json(token);
        }
        return Response.json(user);
    });
    const session = createAuthController();
    const disconnect = session.connect();
    const first = session.login(credentials);
    assert.equal(await session.login(credentials), false);
    await new Promise((resolve) => setTimeout(resolve, 0));
    disconnect();
    release();
    assert.equal(await first, false);
    assert.equal(session.getSnapshot().user, null);
    assert.equal(logins, 1);
});

test("비밀번호 원문의 길이·공백 정책과 ASCII 이메일 형식을 확인", () => {
    assert.equal(isValidPassword("1234567"), false);
    assert.equal(isValidPassword("12345678"), true);
    assert.equal(isValidPassword(" ".repeat(8)), false);
    assert.equal(isValidPassword(" pass word "), true);
    assert.equal(isValidPassword("x".repeat(128)), true);
    assert.equal(isValidPassword("x".repeat(129)), false);
    assert.equal(isValidEmail("user@example.test"), true);
    for (const value of ["user@@example.test", "한글@example.test", "user@", "a b@example.test"])
        assert.equal(isValidEmail(value), false);
});

test("인증 응답은 안전한 유효기간과 목적에 맞는 증명만 수락", () => {
    const proof = "a".repeat(43);
    assert.throws(
        () =>
            validateIssued(
                { verificationId: "bad", expiresInSeconds: 300, resendAfterSeconds: 60 },
                202,
            ),
        ApiError,
    );
    assert.throws(
        () => validateSignupProof({ resetProof: proof, expiresInSeconds: 300 }, 200),
        ApiError,
    );
    assert.throws(
        () => validateResetProof({ resetProof: proof, expiresInSeconds: -1 }, 200),
        ApiError,
    );
    assert.deepEqual(validateSignupProof({ signupProof: proof, expiresInSeconds: 300 }, 200), {
        signupProof: proof,
        expiresInSeconds: 300,
    });
});

test("신규 비밀번호는 8~24자·대문자·숫자·특수문자·공백 금지를 모두 적용하고 기존 로그인 정책은 보존", () => {
    assert.equal(isValidNewPassword("Hello123!"), true);
    for (const value of [
        "Ab12!",
        "a".repeat(25) + "A1!",
        "hello123!",
        "Helloabc!",
        "Hello1234",
        "Hello 123!",
        "Hello123!\t",
    ])
        assert.equal(isValidNewPassword(value), false, value);
    assert.equal(isValidPassword("old-password-123"), true);
    assert.equal(isValidNickname(" 여행자 "), true);
    assert.equal(isValidNickname(" \t "), false);
    assert.equal(isValidNickname("여행\n자"), false);
    assert.equal(isValidNickname("가".repeat(51)), false);
});

test("프로필 사진과 생일·성별 저장 후 회원 상태와 가입일을 유지하고 늦은 응답은 로그인 종료 후 폐기", async (t) => {
    let stored = { ...user };
    let release: (() => void) | undefined;
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        if (path === "/api/auth/refresh") return Response.json(token);
        if (path === "/api/users/me/profile-image") {
            assert.equal(
                JSON.parse(String(options?.body)).dataUrl,
                "data:image/jpeg;base64,fixture",
            );
            stored = { ...stored, profileImageUrl: "/images/profiles/fixture.jpg" };
        }
        if (options?.method === "PATCH") {
            if (release !== undefined)
                await new Promise<void>((resolve) => {
                    release = resolve;
                });
            stored = { ...stored, ...JSON.parse(String(options.body)) };
        }
        return Response.json(stored);
    });
    const session = createAuthController();
    assert.equal(await session.restore(), true);
    await session.updateProfileImage("/brand/avatars/cat.svg", new AbortController().signal);
    assert.equal(session.getSnapshot().user?.profileImageUrl, "/brand/avatars/cat.svg");
    await session.updateProfileDetails(
        { birthDate: "2000-02-29", gender: "FEMALE" },
        new AbortController().signal,
    );
    assert.equal(session.getSnapshot().user?.birthDate, "2000-02-29");
    assert.equal(session.getSnapshot().user?.gender, "FEMALE");
    assert.equal(session.getSnapshot().user?.createdAt, user.createdAt);
    await session.uploadProfileImage(
        "data:image/jpeg;base64,fixture",
        new AbortController().signal,
    );
    assert.equal(session.getSnapshot().user?.profileImageUrl, "/images/profiles/fixture.jpg");
    release = () => {};
    const delayed = session.updateProfileImage(
        "/brand/avatars/dog.svg",
        new AbortController().signal,
    );
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    session.endSession();
    release();
    await assert.rejects(
        delayed,
        (error: unknown) => error instanceof ApiError && error.kind === "aborted",
    );
    assert.equal(session.getSnapshot().user, null);
});
