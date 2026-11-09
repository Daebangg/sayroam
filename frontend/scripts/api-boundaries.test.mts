import assert from "node:assert/strict";
import { test } from "node:test";
import { ESLint } from "eslint";

test("도메인·훅에서 공통 라우터를 우회해 클라이언트·전송 정책을 직접 조합하지 못함", async () => {
    const eslint = new ESLint();
    const forbidden = [
        'import createClient from "openapi-fetch"; void createClient;',
        'import { withRequestPolicy } from "../http.ts"; void withRequestPolicy;',
        'import { request } from "../http.ts"; void request;',
        'import { createApiRouter } from "../router.ts"; void createApiRouter;',
        'import { sendRequest } from "@/api/transport.ts"; void sendRequest;',
        'void import("@/api/http.ts");',
        'void fetch("/api/users/me");',
        'void globalThis.fetch("/api/users/me");',
    ];
    for (const filePath of ["src/api/users/userApi.ts", "src/hooks/api/users/useUserApi.ts"]) {
        for (const source of forbidden) {
            const [result] = await eslint.lintText(source, { filePath });
            assert(
                result?.messages.some((item) => item.ruleId?.startsWith("no-restricted-")),
                source,
            );
        }
    }
    for (const [filePath, source] of [
        ["src/api/users/userApi.ts", 'import { api } from "../api.ts"; void api;'],
        [
            "src/pages/auth/AuthDialog.tsx",
            'import { useEmailFlow } from "@/hooks/api/auth/useEmailFlow"; void useEmailFlow;',
        ],
        [
            "src/hooks/api/users/useUserApi.ts",
            'import { getCurrentUser } from "@/api/users/userApi.ts"; void getCurrentUser;',
        ],
    ]) {
        const [result] = await eslint.lintText(source, { filePath });
        assert.equal(result?.errorCount, 0, JSON.stringify(result?.messages));
    }
    const [hook] = await eslint.lintText('import { request } from "@/api/http.ts"; void request;', {
        filePath: "src/hooks/api/users/useUserApi.ts",
    });
    assert(hook?.messages.some((item) => item.ruleId === "no-restricted-imports"));
});
