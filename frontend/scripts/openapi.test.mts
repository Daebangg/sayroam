import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";
import { renderOpenApi } from "./openapi.mts";

const source = await readFile(new URL("../contracts/openapi.json", import.meta.url), "utf8");

test("같은 저장 명세는 동일한 타입·호출 함수·검증 코드를 생성", async () => {
    const first = await renderOpenApi(source);
    const second = await renderOpenApi(source);
    assert.equal(first.types, second.types);
    assert.deepEqual(first.sdk, second.sdk);
    const sdk = first.sdk.get("sdk.gen.js");
    assert(sdk);
    assert.equal((sdk.match(/export const /g) ?? []).length, 13);
    assert(sdk.includes("responseValidator"));
    assert(first.sdk.has("valibot.gen.js"));
});

test("스키마에 API를 추가하면 별도 수기 함수 없이 호출 함수와 타입이 생김", async () => {
    const document = JSON.parse(source);
    document.paths["/api/generated-example"] = {
        get: {
            operationId: "generatedExample",
            responses: { "204": { description: "empty" } },
        },
    };
    const output = await renderOpenApi(JSON.stringify(document));
    assert(output.sdk.get("sdk.gen.js")?.includes("export const generatedExample"));
    assert(output.types.includes("/api/generated-example"));
});

test("잘못된 JSON·내부 참조·외부 참조는 출력 작성 전에 거절", async () => {
    await assert.rejects(renderOpenApi("{"));
    const document = JSON.parse(source);
    document.components.schemas.Bad = { $ref: "#/components/schemas/Missing" };
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /참조/);
    document.components.schemas.Bad = { $ref: "https://example.invalid/schema.json" };
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /외부/);
});
