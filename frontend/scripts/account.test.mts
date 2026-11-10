/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { ApiError } from "../src/api/apiError.ts";
import { clearAccessToken, setAccessToken } from "../src/api/auth/tokenStore.ts";
import { createAccountController } from "../src/hooks/api/auth/accountController.ts";
import { validateLoginMethods } from "../src/api/users/userResponse.ts";
import type { AccountResult } from "../src/hooks/account.types.ts";

const originalDocument = Object.getOwnPropertyDescriptor(globalThis, "document");
const token = { accessToken: "account-access", tokenType: "Bearer", expiresIn: 900 } as const;
const issued = {
    verificationId: "5c32f7da-462c-4e44-8b28-f1a942853e27",
    expiresInSeconds: 300,
    resendAfterSeconds: 60,
};
const accountProof = "A".repeat(43);
const password = "New-password-456!";
beforeEach(() => {
    setAccessToken(token);
    Object.defineProperty(globalThis, "document", {
        configurable: true,
        value: { cookie: "XSRF-TOKEN=csrf-account" },
    });
});
afterEach(() => {
    clearAccessToken();
    if (originalDocument) Object.defineProperty(globalThis, "document", originalDocument);
    else Reflect.deleteProperty(globalThis, "document");
});

for (const purpose of ["PASSWORD_CHANGE", "ACCOUNT_DELETION"] as const) {
    test(`${purpose}: 목적별 인증과 일회성 증명으로 변경을 한 번만 전송`, async (t) => {
        const calls: { path: unknown; body: unknown; method?: string }[] = [];
        t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
            calls.push({ path, body: JSON.parse(String(options?.body)), method: options?.method });
            assert.equal(
                new Headers(options?.headers).get("Authorization"),
                "Bearer account-access",
            );
            assert.equal(new Headers(options?.headers).get("X-XSRF-TOKEN"), "csrf-account");
            if (path === "/api/auth/account-verifications")
                return Response.json({ ...issued, purpose }, { status: 202 });
            if (path === "/api/auth/account-verifications/verify")
                return Response.json({ purpose, accountProof, expiresInSeconds: 300 });
            return new Response(null, { status: 204 });
        });
        const results: AccountResult[] = [];
        const flow = createAccountController(purpose, (value) => results.push(value));
        assert.equal(await flow.requestCode(), true);
        assert.equal(await flow.requestCode(), false);
        assert.equal(await flow.verifyCode("123456"), true);
        assert.equal(await flow.complete("invalid", false), false);
        assert.equal(await flow.complete(password, true), true);
        assert.equal(await flow.complete(password, true), false);
        assert.deepEqual(results, [{ purpose, outcome: "complete" }]);
        assert.deepEqual(
            calls.map((c) => c.path),
            [
                "/api/auth/account-verifications",
                "/api/auth/account-verifications/verify",
                purpose === "PASSWORD_CHANGE" ? "/api/auth/password" : "/api/auth/account-deletion",
            ],
        );
        assert.deepEqual(calls[0]?.body, { purpose });
        assert.deepEqual(calls[1]?.body, {
            purpose,
            verificationId: issued.verificationId,
            code: "123456",
        });
        assert.deepEqual(
            calls[2]?.body,
            purpose === "PASSWORD_CHANGE"
                ? { accountProof, newPassword: password }
                : { accountProof },
        );
        assert.equal(calls[2]?.method, purpose === "PASSWORD_CHANGE" ? "PATCH" : "POST");
        flow.dispose();
    });
}

test("발송 중 상태를 구분하고 중복 요청과 닫힌 요청의 늦은 응답을 막음", async (t) => {
    let release!: (value: Response) => void;
    const mock = t.mock.method(
        globalThis,
        "fetch",
        () =>
            new Promise<Response>((resolve) => {
                release = resolve;
            }),
    );
    const flow = createAccountController("ACCOUNT_DELETION", () =>
        assert.fail("발송은 삭제 결과가 아님"),
    );
    const sending = flow.requestCode();
    assert.equal(flow.getSnapshot().sending, true);
    assert.equal(await flow.requestCode(), false);
    while (!release) await new Promise((resolve) => setImmediate(resolve));
    flow.dispose();
    release(Response.json({ ...issued, purpose: "ACCOUNT_DELETION" }, { status: 202 }));
    assert.equal(await sending, false);
    assert.equal(flow.getSnapshot().stage, "request");
    assert.equal(mock.mock.callCount(), 1);
});

test("다른 목적의 발송 응답과 인증 증명을 거절", async (t) => {
    let confirmation = false;
    t.mock.method(globalThis, "fetch", async (path: unknown) => {
        if (path === "/api/auth/account-verifications")
            return Response.json(
                {
                    ...issued,
                    purpose: confirmation ? "PASSWORD_CHANGE" : "ACCOUNT_DELETION",
                    resendAfterSeconds: 0,
                },
                { status: 202 },
            );
        return Response.json({ purpose: "ACCOUNT_DELETION", accountProof, expiresInSeconds: 300 });
    });
    const flow = createAccountController("PASSWORD_CHANGE", () => assert.fail("다른 목적"));
    assert.equal(await flow.requestCode(), false);
    assert.equal(flow.getSnapshot().message, "uncertain");
    confirmation = true;
    assert.equal(await flow.requestCode(), true);
    assert.equal(await flow.verifyCode("123456"), false);
    assert.equal(flow.getSnapshot().stage, "request");
    assert.equal(await flow.complete(password, true), false);
});

for (const failure of ["network", "server"] as const) {
    test(`${failure}: 최종 결과 불확실을 알리고 변경을 반복하지 않음`, async (t) => {
        let mutations = 0;
        t.mock.method(globalThis, "fetch", async (path: unknown) => {
            if (path === "/api/auth/account-verifications")
                return Response.json({ ...issued, purpose: "PASSWORD_CHANGE" }, { status: 202 });
            if (path === "/api/auth/account-verifications/verify")
                return Response.json({
                    purpose: "PASSWORD_CHANGE",
                    accountProof,
                    expiresInSeconds: 300,
                });
            mutations++;
            if (failure === "network") throw new TypeError("network unavailable");
            return Response.json(
                { status: 503, code: "PASSWORD_CHANGE_OUTCOME_UNKNOWN" },
                { status: 503 },
            );
        });
        const results: AccountResult[] = [];
        const flow = createAccountController("PASSWORD_CHANGE", (result) => results.push(result));
        await flow.requestCode();
        await flow.verifyCode("123456");
        assert.equal(await flow.complete(password, false), false);
        assert.equal(await flow.complete(password, false), false);
        assert.equal(mutations, 1);
        assert.deepEqual(results, [{ purpose: "PASSWORD_CHANGE", outcome: "uncertain" }]);
    });
}

test("새 로그인 이후 이전 계정의 증명을 재사용하지 않음", async (t) => {
    const mock = t.mock.method(globalThis, "fetch", async (path: unknown) =>
        path === "/api/auth/account-verifications"
            ? Response.json({ ...issued, purpose: "ACCOUNT_DELETION" }, { status: 202 })
            : Response.json({ purpose: "ACCOUNT_DELETION", accountProof, expiresInSeconds: 300 }),
    );
    const flow = createAccountController("ACCOUNT_DELETION", () => assert.fail("이전 계정 요청"));
    await flow.requestCode();
    await flow.verifyCode("123456");
    setAccessToken(token);
    assert.equal(await flow.complete("", true), false);
    assert.equal(mock.mock.callCount(), 2);
});

test("연결된 로그인 수단은 불리언과 지원 공급자만 허용", () => {
    assert.deepEqual(
        validateLoginMethods({ passwordEnabled: false, socialProviders: ["GOOGLE", "NAVER"] }, 200),
        { passwordEnabled: false, socialProviders: ["GOOGLE", "NAVER"] },
    );
    for (const value of [
        { passwordEnabled: "false", socialProviders: [] },
        { passwordEnabled: true, socialProviders: ["GITHUB"] },
        { passwordEnabled: true, socialProviders: ["GOOGLE", "GOOGLE"] },
    ])
        assert.throws(() => validateLoginMethods(value, 200), ApiError);
});
