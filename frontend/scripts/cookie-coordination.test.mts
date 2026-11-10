/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { withAuthCookieLock } from "../src/api/auth/cookieCoordination.ts";

const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");
const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "localStorage");
const originalLocks = Object.getOwnPropertyDescriptor(navigator, "locks");
let state: Map<string, string>;

beforeEach(() => {
    state = new Map();
    let tail: Promise<unknown> = Promise.resolve();
    Object.defineProperty(globalThis, "window", { configurable: true, value: {} });
    Object.defineProperty(globalThis, "localStorage", {
        configurable: true,
        value: {
            getItem: (key: string) => state.get(key) ?? null,
            setItem: (key: string, value: string) => state.set(key, value),
        },
    });
    Object.defineProperty(navigator, "locks", {
        configurable: true,
        value: {
            request: <T,>(_name: string, execute: () => Promise<T>): Promise<T> => {
                const result = tail.then(execute);
                tail = result.catch(() => undefined);
                return result;
            },
        },
    });
});
afterEach(() => {
    for (const [target, key, original] of [
        [globalThis, "window", originalWindow],
        [globalThis, "localStorage", originalStorage],
        [navigator, "locks", originalLocks],
    ] as const) {
        if (original) Object.defineProperty(target, key, original);
        else Reflect.deleteProperty(target, key);
    }
});

const authRequired = (error: unknown): boolean =>
    error instanceof ApiError && error.kind === "authentication-required";

test("앞 갱신의 응답이 유실되면 이미 대기 중인 다른 갱신을 전송하지 않음", async () => {
    let calls = 0;
    const first = withAuthCookieLock("refresh", async () => {
        calls++;
        throw new TypeError("response lost");
    });
    const queued = withAuthCookieLock("refresh", async () => {
        calls++;
    });
    await assert.rejects(first, TypeError);
    await assert.rejects(queued, authRequired);
    assert.equal(calls, 1);
    assert([...state.values()].every((value) => value.startsWith("blocked:")));
});

test("실패한 로그아웃도 복원을 차단하고 명시적인 새 로그인만 차단 해제", async () => {
    await assert.rejects(
        withAuthCookieLock("logout", async () => {
            throw new Error("503");
        }),
    );
    await assert.rejects(
        withAuthCookieLock("refresh", async () => assert.fail("자동 복원 금지")),
        authRequired,
    );
    await withAuthCookieLock("login", async () => undefined);
    assert([...state.values()].every((value) => value.startsWith("ready:")));
    assert.equal(await withAuthCookieLock("refresh", async () => "issued"), "issued");
    assert(![...state.values()].some((value) => value.includes("issued")));
});

test("공유 저장소를 읽을 수 없으면 쿠키 변경 요청을 전송하지 않음", async () => {
    Object.defineProperty(globalThis, "localStorage", {
        configurable: true,
        value: {
            getItem: () => {
                throw new Error("blocked");
            },
        },
    });
    await assert.rejects(
        withAuthCookieLock("login", async () => assert.fail("전송 금지")),
        (error: unknown) =>
            error instanceof ApiError && error.kind === "authentication-unavailable",
    );
});

test("OAuth 인증 시작은 Refresh 쿠키의 자동 복원 상태를 바꾸지 않음", async () => {
    await withAuthCookieLock("login", async () => undefined);
    const before = [...state.values()];
    await withAuthCookieLock("oauth", async () => ({
        authorizationUrl: "https://accounts.google.com/",
    }));
    assert.deepEqual([...state.values()], before);
});

test("소셜 가입 대기는 이전 계정의 자동 복원을 막고 실제 인증 성공만 해제", async () => {
    await withAuthCookieLock("social", async () => ({ status: "SIGNUP_REQUIRED" }));
    await assert.rejects(
        withAuthCookieLock("refresh", async () => assert.fail("do not restore old account")),
        authRequired,
    );
    await withAuthCookieLock("social", async () => ({ status: "AUTHENTICATED" }));
    assert([...state.values()].every((value) => value.startsWith("ready:")));
});
