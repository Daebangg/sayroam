import { spawnSync } from "node:child_process";
import { mkdir, mkdtemp, readFile, readdir, rename, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { loadEnvFile } from "node:process";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { createConfig } from "@redocly/openapi-core";
import { createClient } from "@hey-api/openapi-ts";
import openapiTS, { astToString } from "openapi-typescript";
import ts from "typescript";
import { format, resolveConfig } from "prettier";

const frontendDir = fileURLToPath(new URL("../", import.meta.url));
const snapshotPath = resolve(frontendDir, "contracts/openapi.json");
const typesPath = resolve(frontendDir, "src/api/generated/schema.ts");
const sdkPath = resolve(frontendDir, "src/api/generated/sdk");

async function readTree(directory: string, prefix = ""): Promise<Map<string, string>> {
    const files = new Map<string, string>();
    const entries = await readdir(directory, { withFileTypes: true }).catch((error: unknown) => {
        if (isObject(error) && error.code === "ENOENT") return [];
        throw error;
    });
    for (const entry of entries) {
        const name = prefix + entry.name;
        if (entry.isDirectory()) {
            for (const [path, source] of await readTree(join(directory, entry.name), name + "/")) {
                files.set(path, source);
            }
        } else files.set(name, await readFile(join(directory, entry.name), "utf8"));
    }
    return files;
}

// JSON의 int64는 기존 앱과 같은 number로 받고 안전한 정수 범위를 검증한다.
function sdkDocument(value: unknown): unknown {
    if (Array.isArray(value)) return value.map(sdkDocument);
    if (!isObject(value)) return value;
    const result = Object.fromEntries(
        Object.entries(value).map(([key, item]) => [key, sdkDocument(item)]),
    );
    if (value.type === "integer" && value.format === "int64") {
        delete result.format;
        result.minimum = Math.max(
            typeof value.minimum === "number" ? value.minimum : -Infinity,
            Number.MIN_SAFE_INTEGER,
        );
        result.maximum = Math.min(
            typeof value.maximum === "number" ? value.maximum : Infinity,
            Number.MAX_SAFE_INTEGER,
        );
    }
    return result;
}

async function renderSdk(document: unknown): Promise<Map<string, string>> {
    const temporary = await mkdtemp(join(tmpdir(), "sayroam-sdk-"));
    try {
        const input = sdkDocument(document);
        if (!isObject(input)) throw new Error("SDK 생성에는 OpenAPI 객체가 필요합니다.");
        await createClient({
            input,
            output: { path: temporary, postProcess: [], module: { extension: ".js" } },
            plugins: [
                "@hey-api/typescript",
                { name: "@hey-api/client-fetch", throwOnError: true },
                {
                    name: "@hey-api/sdk",
                    responseStyle: "data",
                    validator: { request: false, response: true },
                },
                "valibot",
            ],
        });
        const sources = await readTree(temporary);
        if (
            !sources.has("sdk.gen.ts") ||
            !sources.has("types.gen.ts") ||
            !sources.has("valibot.gen.ts")
        ) {
            throw new Error("SDK·타입·응답 검증 코드가 모두 생성되지 않았습니다.");
        }
        const compiled = join(temporary, "compiled");
        // 외부 SDK 런타임은 자체 경계에서 컴파일한다. 앱의 strict 옵션은 유지한다.
        const program = ts.createProgram(
            [...sources.keys()].map((name) => join(temporary, name)),
            {
                target: ts.ScriptTarget.ES2023,
                module: ts.ModuleKind.ESNext,
                moduleResolution: ts.ModuleResolutionKind.Bundler,
                strict: true,
                noUncheckedIndexedAccess: true,
                skipLibCheck: true,
                declaration: true,
                rootDir: temporary,
                outDir: compiled,
                paths: { valibot: [resolve(frontendDir, "node_modules/valibot/dist/index.d.mts")] },
            },
        );
        const diagnostics = ts.getPreEmitDiagnostics(program);
        if (diagnostics.length) {
            throw new Error(
                ts.formatDiagnosticsWithColorAndContext(diagnostics, {
                    getCurrentDirectory: () => frontendDir,
                    getCanonicalFileName: (name) => name,
                    getNewLine: () => "\n",
                }),
            );
        }
        if (program.emit().emitSkipped) throw new Error("SDK 컴파일에 실패했습니다.");
        const config = await resolveConfig(resolve(frontendDir, ".prettierrc.json"));
        const files = await readTree(compiled);
        for (const [name, source] of files) {
            files.set(name, await format(source, { ...config, parser: "typescript" }));
        }
        return files;
    } finally {
        await rm(temporary, { recursive: true, force: true });
    }
}

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function validateDocument(document: unknown): asserts document is Record<string, unknown> {
    if (
        !isObject(document) ||
        typeof document.openapi !== "string" ||
        !/^3\.[01]\.\d+$/.test(document.openapi) ||
        !isObject(document.info) ||
        typeof document.info.title !== "string" ||
        typeof document.info.version !== "string" ||
        !isObject(document.paths)
    ) {
        throw new Error("OpenAPI 3.0/3.1 JSON 명세와 info.title·info.version·paths가 필요합니다.");
    }

    function visit(value: unknown): void {
        if (Array.isArray(value)) {
            value.forEach(visit);
            return;
        }
        if (!isObject(value)) return;
        if ("$ref" in value) {
            const reference = value.$ref;
            if (typeof reference !== "string" || !reference.startsWith("#/")) {
                throw new Error(
                    "오프라인 재생성을 위해 외부 $ref 없이 내보낸 단일 명세가 필요합니다.",
                );
            }
            let target: unknown = document;
            for (const segment of reference.slice(2).split("/")) {
                const key = decodeURIComponent(segment).replace(/~1/g, "/").replace(/~0/g, "~");
                if (!isObject(target) || !Object.hasOwn(target, key)) {
                    throw new Error(`명세의 참조를 찾을 수 없습니다: ${reference}`);
                }
                target = target[key];
            }
        }
        Object.values(value).forEach(visit);
    }
    visit(document);
}

function sortKeys(value: unknown): unknown {
    if (Array.isArray(value)) return value.map(sortKeys);
    if (!isObject(value)) return value;
    return Object.fromEntries(
        Object.keys(value)
            .sort()
            .map((key) => [key, sortKeys(value[key])]),
    );
}

export async function renderOpenApi(
    source: string,
): Promise<{ snapshot: string; types: string; sdk: Map<string, string> }> {
    let document: unknown;
    try {
        document = JSON.parse(source);
    } catch {
        throw new Error(
            "OpenAPI 응답을 JSON으로 해석할 수 없습니다. 문서 주소와 서버 응답을 확인하세요.",
        );
    }
    validateDocument(document);
    const config = await resolveConfig(resolve(frontendDir, ".prettierrc.json"));
    const snapshot = await format(JSON.stringify(sortKeys(document)), {
        ...config,
        parser: "json",
    });
    const redocly = await createConfig({
        extends: [],
        rules: {
            struct: "error",
            "operation-operationId-unique": "error",
            "no-unresolved-refs": "error",
        },
    });
    const ast = await openapiTS(snapshot, { alphabetize: true, redocly });
    const types = await format(
        "/** OpenAPI에서 자동 생성합니다. 직접 수정하지 말고 pnpm api:generate를 실행하세요. */\n" +
            astToString(ast),
        { ...config, parser: "typescript" },
    );
    return { snapshot, types, sdk: await renderSdk(document) };
}

async function writeOutput(path: string, contents: string): Promise<void> {
    await mkdir(dirname(path), { recursive: true });
    const temporaryPath = `${path}.${process.pid}.tmp`;
    try {
        await writeFile(temporaryPath, contents);
        await rename(temporaryPath, path);
    } finally {
        await rm(temporaryPath, { force: true });
    }
}

async function main(): Promise<void> {
    const [command, ...args] = process.argv.slice(2);
    if (command !== "pull" && command !== "generate" && command !== "check") {
        throw new Error("사용법: openapi.mts pull [--url 주소 | --file 파일] | generate | check");
    }
    const { values } = parseArgs({
        args,
        options: { url: { type: "string" }, file: { type: "string" } },
    });
    if ((values.url && values.file) || (command !== "pull" && (values.url || values.file))) {
        throw new Error("--url 또는 --file은 api:pull에서 하나만 지정할 수 있습니다.");
    }

    let inputPath = snapshotPath;
    if (command === "pull") {
        if (values.file) {
            inputPath = resolve(frontendDir, values.file);
        } else {
            try {
                loadEnvFile(resolve(frontendDir, ".env.local"));
            } catch (error) {
                if (!isObject(error) || error.code !== "ENOENT") throw error;
            }
            const endpoint =
                values.url ?? process.env.OPENAPI_URL ?? "http://localhost:8080/v3/api-docs";
            const url = new URL(endpoint);
            if (url.protocol !== "http:" && url.protocol !== "https:") {
                throw new Error("OpenAPI 주소는 http 또는 https여야 합니다.");
            }
            const backendDir = resolve(frontendDir, "../backend");
            const gradleArgs = ["-p", backendDir, "exportOpenApi", `-PapiDocsUrl=${url.href}`];
            let executable = resolve(backendDir, "gradlew");
            if (process.platform === "win32") {
                // 배치 셸을 거치지 않아 경로의 공백과 URL의 셸 문자를 그대로 전달합니다.
                executable = process.env.JAVA_HOME
                    ? resolve(process.env.JAVA_HOME, "bin/java.exe")
                    : "java.exe";
                gradleArgs.unshift(
                    "-classpath",
                    resolve(backendDir, "gradle/wrapper/gradle-wrapper.jar"),
                    "org.gradle.wrapper.GradleWrapperMain",
                );
            }
            const result = spawnSync(executable, gradleArgs, {
                cwd: frontendDir,
                stdio: ["inherit", "pipe", "pipe"],
                encoding: "utf8",
                shell: false,
            });
            process.stdout.write(result.stdout ?? "");
            process.stderr.write(result.stderr ?? "");
            if (result.error || result.status !== 0) {
                const details = result.stderr
                    ?.match(/\* What went wrong:\s*([\s\S]*?)(?:\n\s*\n|\n\* Try:)/)?.[1]
                    ?.trim();
                throw new Error(
                    `백엔드 명세 내보내기 실패 (${url.href}): ${details ?? result.error?.message ?? "서버와 OPENAPI_URL 또는 --url을 확인하세요."}`,
                );
            }
            inputPath = resolve(backendDir, "build/openapi/openapi.json");
        }
    }

    let source: string;
    try {
        source = await readFile(inputPath, "utf8");
    } catch {
        throw new Error(
            `명세 파일을 읽을 수 없습니다: ${inputPath}. 먼저 pnpm api:pull을 실행하세요.`,
        );
    }
    // 검증과 타입 생성을 모두 마친 뒤 기존 계약 파일을 교체합니다.
    const output = await renderOpenApi(source);
    if (command === "check") {
        const savedTypes = await readFile(typesPath, "utf8").catch(() => "");
        const savedSdk = await readTree(sdkPath);
        const sdkMatches =
            savedSdk.size === output.sdk.size &&
            [...output.sdk].every(([name, source]) => savedSdk.get(name) === source);
        if (source !== output.snapshot || savedTypes !== output.types || !sdkMatches) {
            throw new Error(
                "명세·생성 타입·SDK가 최신 결과와 다릅니다. pnpm api:generate 후 변경을 확인하세요.",
            );
        }
        console.log("OpenAPI 명세·생성 타입·SDK가 일치합니다.");
        return;
    }
    await writeOutput(snapshotPath, output.snapshot);
    await writeOutput(typesPath, output.types);
    const previousSdk = await readTree(sdkPath);
    for (const [name, source] of output.sdk) await writeOutput(join(sdkPath, name), source);
    for (const name of previousSdk.keys()) {
        if (!output.sdk.has(name)) await rm(join(sdkPath, name));
    }
    console.log(
        "OpenAPI 타입·SDK·응답 검증 생성 완료: contracts/openapi.json → src/api/generated/",
    );
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    main().catch((error: unknown) => {
        console.error(`[OpenAPI] ${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    });
}
