import { spawnSync } from "node:child_process";
import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { loadEnvFile } from "node:process";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { createConfig } from "@redocly/openapi-core";
import openapiTS, { astToString } from "openapi-typescript";
import { format, resolveConfig } from "prettier";

const frontendDir = fileURLToPath(new URL("../", import.meta.url));
const snapshotPath = resolve(frontendDir, "contracts/openapi.json");
const typesPath = resolve(frontendDir, "src/api/generated/schema.ts");
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

export async function renderOpenApi(source: string): Promise<{ snapshot: string; types: string }> {
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
    return { snapshot, types };
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
        if (source !== output.snapshot || savedTypes !== output.types) {
            throw new Error(
                "명세·생성 타입이 최신 결과와 다릅니다. pnpm api:generate 후 변경을 확인하세요.",
            );
        }
        console.log("OpenAPI 명세·생성 타입이 일치합니다.");
        return;
    }
    await writeOutput(snapshotPath, output.snapshot);
    await writeOutput(typesPath, output.types);
    console.log("OpenAPI 타입 생성 완료: contracts/openapi.json → src/api/generated/schema.ts");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    main().catch((error: unknown) => {
        console.error(`[OpenAPI] ${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    });
}
