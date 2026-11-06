import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";
import { renderOpenApi } from "./openapi.mts";

const source = await readFile(new URL("../contracts/openapi.json", import.meta.url), "utf8");

test("같은 저장 명세는 동일한 스냅샷과 타입 파일을 생성", async () => {
    const first = await renderOpenApi(source);
    const second = await renderOpenApi(source);
    assert.equal(first.types, second.types);
    assert.equal(first.snapshot, second.snapshot);
});

test("스키마에 API를 추가하면 경로·메서드가 타입에 반영됨", async () => {
    const document = JSON.parse(source);
    document.paths["/api/generated-example"] = {
        get: {
            operationId: "generatedExample",
            responses: { "204": { description: "empty" } },
        },
    };
    const output = await renderOpenApi(JSON.stringify(document));
    assert(output.types.includes("/api/generated-example"));
    assert(output.types.includes("generatedExample"));
});

test("잘못된 JSON·내부 참조·외부 참조는 출력 작성 전에 거절", async () => {
    await assert.rejects(renderOpenApi("{"));
    const document = JSON.parse(source);
    document.components.schemas.Bad = { $ref: "#/components/schemas/Missing" };
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /참조/);
    document.components.schemas.Bad = { $ref: "https://example.invalid/schema.json" };
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /외부/);
});
