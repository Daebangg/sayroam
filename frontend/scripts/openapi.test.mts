import assert from "node:assert/strict";
import { mkdtemp, readFile, readdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { test } from "node:test";
import { createApiRouter } from "../src/api/router.ts";
import type { apiRoutes } from "../src/api/generated/schema.ts";
import { renderOpenApi } from "./openapi.mts";

const source = await readFile(new URL("../contracts/openapi.json", import.meta.url), "utf8");

async function generatedRoutes(input: string): Promise<typeof apiRoutes> {
    const { types } = await renderOpenApi(input);
    const directory = await mkdtemp(join(tmpdir(), "sayroam-route-test-"));
    try {
        const filename = join(directory, "schema.ts");
        await writeFile(filename, types);
        return (await import(pathToFileURL(filename).href)).apiRoutes;
    } finally {
        await rm(directory, { recursive: true, force: true });
    }
}

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

test("명세에 추가한 API는 별도 호출 함수 없이 경로·쿼리를 직렬화해 실행", async (t) => {
    const document = JSON.parse(source);
    document.paths["/api/generated-example/{id}"] = {
        get: {
            operationId: "generatedExample",
            security: [],
            parameters: [
                { in: "path", name: "id", required: true, schema: { type: "string" } },
                { in: "query", name: "limit", required: true, schema: { type: "integer" } },
            ],
            responses: {
                "200": {
                    description: "value",
                    content: {
                        "application/json": {
                            schema: {
                                type: "object",
                                properties: { value: { type: "integer" } },
                                required: ["value"],
                            },
                        },
                    },
                },
            },
        },
    };
    const router = createApiRouter(await generatedRoutes(JSON.stringify(document)));
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        assert.equal(path, "/api/generated-example/a%2Fb?limit=2");
        assert.equal(options?.method, "GET");
        assert.equal(new Headers(options?.headers).get("Authorization"), null);
        return Response.json({ value: 3 });
    });
    assert.deepEqual(
        await Reflect.apply(router.call, router, [
            "generatedExample",
            {
                params: {
                    path: { id: "a/b" },
                    query: { limit: 2 },
                },
            },
        ]),
        { value: 3 },
    );
});

test("전역 인증 상속과 엔드포인트의 공개 인증 재정의 반영", async () => {
    const document = JSON.parse(source);
    document.security = [{ bearerAuth: [], csrfToken: [] }];
    delete document.paths["/api/users/me"].get.security;
    document.paths["/api/auth/csrf"].get.security = [];
    const routes = await generatedRoutes(JSON.stringify(document));
    assert.equal(routes.me.auth, "required");
    assert.equal(routes.me.csrf, true);
    assert.equal(routes.csrf.auth, "none");
    assert.equal(routes.csrf.csrf, false);
});

test("성공 응답의 내부 참조를 경로표에 반영", async () => {
    const document = JSON.parse(source);
    document.components.responses.EmptySuccess = { description: "empty" };
    document.paths["/api/auth/logout"].post.responses["204"] = {
        $ref: "#/components/responses/EmptySuccess",
    };
    const routes = await generatedRoutes(JSON.stringify(document));
    assert.deepEqual(routes.logout.responses, { "204": false });
});

test("누락·중복 operationId와 해석할 수 없는 인증 규칙은 생성 전에 거절", async () => {
    const document = JSON.parse(source);
    delete document.paths["/api/users/me"].get.operationId;
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /operationId/);
    document.paths["/api/users/me"].get.operationId = "login";
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /operationId/);
    document.paths["/api/users/me"].get.operationId = "me";
    document.paths["/api/users/me"].get.security = [{ bearerAuth: [] }, {}];
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /인증 대안/);
    document.paths["/api/users/me"].get.security = [{ other: [] }];
    document.components.securitySchemes.other = { type: "http", scheme: "basic" };
    await assert.rejects(renderOpenApi(JSON.stringify(document)), /인증 방식/);
});

test("생성 결과물은 schema.ts 한 파일이며 브라우저 콜백은 경로표에서 제외", async () => {
    assert.deepEqual(await readdir(new URL("../src/api/generated/", import.meta.url)), [
        "schema.ts",
    ]);
    const routes = await generatedRoutes(source);
    assert.equal(routes.me.path, "/api/users/me");
    assert.equal(routes.startSocialLogin.path, "/api/auth/social/{provider}/authorizations");
    assert.equal(routes.exchangeSocialLoginResult.path, "/api/auth/social/results/exchange");
    assert.equal(routes.completeSocialSignup.path, "/api/auth/social/signup");
    assert.equal(Object.hasOwn(routes, "socialLoginCallback"), false);
});
