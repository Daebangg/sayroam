import assert from "node:assert/strict";
import { test } from "node:test";
import { ESLint } from "eslint";

test("도메인·훅에서 생성 SDK 배럴·클라이언트·수기 전송으로 우회하지 못함", async () => {
    const eslint = new ESLint();
    const forbidden = [
        'import { me } from "../generated/sdk/index.js"; void me;',
        'import { client } from "../generated/sdk/client/client.gen.js"; void client;',
        'import { request } from "../http.ts"; void request;',
        'import { sendRequest } from "@/api/transport.ts"; void sendRequest;',
        'void import("../generated/sdk/index.js");',
        'void import("@/api/http.ts");',
        'void fetch("/api/users/me");',
        'void globalThis.fetch("/api/users/me");',
    ];
    for (const filePath of ["src/api/users/boundary.ts", "src/hooks/api/users/boundary.ts"]) {
        for (const source of forbidden) {
            const [result] = await eslint.lintText(source, { filePath });
            assert(
                result?.messages.some((item) => item.ruleId?.startsWith("no-restricted-")),
                source,
            );
        }
        const [allowed] = await eslint.lintText('import { me } from "@/api/sdk.ts"; void me;', {
            filePath,
        });
        assert.equal(allowed?.errorCount, 0);
    }
});
