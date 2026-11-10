/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { clearAccessToken, getAccessToken } from "../src/api/auth/tokenStore.ts";
import {
    validateAuthorization,
    validateProviders,
    validateSocialExchange,
    authorizationMatches,
} from "../src/api/auth/socialResponse.ts";
import { createAuthController } from "../src/hooks/api/auth/authController.ts";
import { parseSocialReturn, safeSocialReturn } from "../src/routing/socialReturn.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const token = { accessToken: "social-access", tokenType: "Bearer", expiresIn: 900 };
const user = {
    userId: 1,
    email: "member@example.test",
    nickname: "여행자",
    profileImageUrl: null,
    gender: null,
    birthYear: null,
    role: "USER",
};
const signup = (required = false) => ({
    status: "SIGNUP_REQUIRED",
    login: null,
    signup: {
        signupTicket: "T".repeat(43),
        expiresInSeconds: 600,
        emailVerificationRequired: required,
        suggestedEmail: required ? null : "member@example.test",
    },
});
beforeEach(() => {
    clearAccessToken();
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=test-csrf" },
    });
});
afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

test("활성 공급자만 받고 임의 인가 주소·불완전 로그인 결과를 거절", () => {
    assert.deepEqual(validateProviders({ providers: ["GOOGLE", "GOOGLE"] }, 200), {
        providers: ["GOOGLE"],
    });
    assert.throws(() => validateProviders({ providers: ["UNKNOWN"] }, 200));
    for (const address of [
        "javascript:alert(1)",
        "https://accounts.google.com.evil.test/o/oauth2/v2/auth",
        "https://evil.test",
        "https://accounts.google.com/o/oauth2/v2/auth#leak",
    ])
        assert.throws(() =>
            validateAuthorization({ authorizationUrl: address, expiresInSeconds: 300 }, 200),
        );
    const address = "https://accounts.google.com/o/oauth2/v2/auth?state=test";
    assert.equal(
        authorizationMatches(
            "GOOGLE",
            validateAuthorization({ authorizationUrl: address, expiresInSeconds: 300 }, 200)
                .authorizationUrl,
        ),
        true,
    );
    assert.equal(authorizationMatches("KAKAO", address), false);
    assert.throws(() =>
        validateSocialExchange({ status: "AUTHENTICATED", login: {}, signup: null }, 200),
    );
    assert.throws(() => validateSocialExchange({ ...signup(), login: token }, 200));
    assert.throws(() =>
        validateSocialExchange(
            { ...signup(), signup: { ...signup().signup, signupTicket: "invalid" } },
            200,
        ),
    );
});

test("복귀 경로는 로컬 화면만 허용하고 결과 중복·만료·다른 탭의 로그인 변경을 거절", () => {
    const code = "C".repeat(43),
        now = 1000000;
    const url = new URL("https://app.example.test/#social_result=" + code);
    const saved = JSON.stringify({
        returnPath: "/trips?room=1&modal=auth#itinerary",
        createdAt: now - 1000,
        cookieState: "ready:g",
    });
    assert.deepEqual(parseSocialReturn(url, saved, "ready:g", now), {
        code,
        returnPath: "/trips?room=1#itinerary",
    });
    assert.equal(parseSocialReturn(url, saved, "blocked:logout", now)?.code, "");
    assert.equal(parseSocialReturn(url, saved, "ready:g", now + 16 * 60 * 1000)?.code, "");
    assert.equal(
        parseSocialReturn(new URL(url.href + "&social_result=" + code), saved, "ready:g", now)
            ?.code,
        "",
    );
    for (const path of [
        "https://evil.test",
        "//evil.test",
        "/api/users/me",
        "/\\evil.test",
        "/missing",
    ])
        assert.equal(safeSocialReturn(path), "/");
    assert.equal(
        parseSocialReturn(new URL("https://app.example.test/#itinerary"), saved, "ready:g", now),
        null,
    );
});

test("StrictMode 효과 재연결에도 결과 POST는 한 번이며 신규 가입에는 토큰을 만들지 않음", async (t) => {
    const calls: string[] = [];
    let release: (value: Response) => void = () => assert.fail("request not started");
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        calls.push(String(path));
        return new Promise<Response>((resolve) => {
            release = resolve;
        });
    });
    const session = createAuthController();
    const disconnect = session.connect();
    const first = session.exchangeSocial("A".repeat(43));
    await new Promise((resolve) => setImmediate(resolve));
    disconnect();
    const finish = session.connect();
    const second = session.exchangeSocial("A".repeat(43));
    release(Response.json(signup(true)));
    await first;
    await second;
    assert.deepEqual(calls, ["/api/auth/social/results/exchange"]);
    assert.equal(session.getSnapshot().socialSignup?.emailVerificationRequired, true);
    assert.equal(session.getSnapshot().user, null);
    assert.equal(getAccessToken(), undefined);
    finish();
});

test("기존 소셜 회원은 공통 토큰·회원 조회를 거쳐 로그인 상태 하나를 갱신", async (t) => {
    const calls: string[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        calls.push(String(path));
        return Response.json(
            path === "/api/users/me"
                ? user
                : { status: "AUTHENTICATED", login: token, signup: null },
        );
    });
    const session = createAuthController();
    assert.equal(await session.exchangeSocial("B".repeat(43)), true);
    assert.equal(session.getSnapshot().status, "authenticated");
    assert.deepEqual(session.getSnapshot().user, user);
    assert.equal(getAccessToken(), token.accessToken);
    assert.deepEqual(calls, ["/api/auth/social/results/exchange", "/api/users/me"]);
});

for (const required of [false, true])
    test(`추가 가입은 닉네임과 필요한 이메일 증명만 전달하고 로그인함 (${required})`, async (t) => {
        const bodies: unknown[] = [];
        t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
            if (path === "/api/users/me") return Response.json(user);
            bodies.push(JSON.parse(String(options?.body)));
            return Response.json(
                path === "/api/auth/social/results/exchange" ? signup(required) : token,
            );
        });
        const session = createAuthController();
        await session.exchangeSocial((required ? "D" : "E").repeat(43));
        const input = {
            nickname: "여행자",
            ...(required ? { email: "member@example.test", signupProof: "P".repeat(43) } : {}),
        };
        assert.equal(await session.completeSocial(input), true);
        assert.deepEqual(bodies[1], { ...input, signupTicket: "T".repeat(43) });
        assert.equal(session.getSnapshot().socialSignup, undefined);
        assert.equal(session.getSnapshot().status, "authenticated");
    });

test("취소·충돌·소비된 결과는 복구 문구로 안내하고 같은 결과를 자동 재전송하지 않음", async (t) => {
    let calls = 0;
    t.mock.method(globalThis, "fetch", async () => {
        calls++;
        return Response.json({ status: 400, code: "SOCIAL_AUTH_CANCELLED" }, { status: 400 });
    });
    const session = createAuthController();
    assert.equal(await session.exchangeSocial("F".repeat(43)), false);
    assert.equal(session.getSnapshot().message, "socialCancelled");
    assert.equal(await session.exchangeSocial("F".repeat(43)), false);
    assert.equal(calls, 1);
    const mock = t.mock.method(globalThis, "fetch", async () =>
        Response.json({ status: 409, code: "EMAIL_ALREADY_REGISTERED" }, { status: 409 }),
    );
    assert.equal(await session.exchangeSocial("G".repeat(43)), false);
    assert.equal(session.getSnapshot().message, "socialConflict");
    assert.equal(mock.mock.calls.length, 1);
    assert.equal(getAccessToken(), undefined);
});

test("잘못된 복귀 코드는 서버 요청 없이 새 소셜 인증을 안내", async (t) => {
    const mock = t.mock.method(globalThis, "fetch", async () => assert.fail("do not send"));
    const session = createAuthController();
    assert.equal(await session.exchangeSocial("invalid"), false);
    assert.equal(session.getSnapshot().message, "socialExpired");
    assert.equal(mock.mock.calls.length, 0);
});

test("소셜 인증 응답 대기 중 로그아웃되면 늦은 토큰으로 계정을 되살리지 않음", async (t) => {
    let release: (value: Response) => void = () => assert.fail("request not started");
    t.mock.method(
        globalThis,
        "fetch",
        async () =>
            new Promise<Response>((resolve) => {
                release = resolve;
            }),
    );
    const session = createAuthController();
    const pending = session.exchangeSocial("H".repeat(43));
    await new Promise((resolve) => setImmediate(resolve));
    clearAccessToken();
    release(Response.json({ status: "AUTHENTICATED", login: token, signup: null }));
    assert.equal(await pending, false);
    assert.equal(getAccessToken(), undefined);
});
