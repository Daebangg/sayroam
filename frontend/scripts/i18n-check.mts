import { readFile } from "node:fs/promises";

const errors: string[] = [];
function flatten(
    value: unknown,
    prefix = "",
    result = new Map<string, string>(),
): Map<string, string> {
    if (typeof value === "string" && value.trim()) {
        result.set(prefix, value);
    } else if (value !== null && typeof value === "object" && !Array.isArray(value)) {
        for (const [key, child] of Object.entries(value)) {
            flatten(child, prefix ? `${prefix}.${key}` : key, result);
        }
    } else {
        errors.push(`${prefix}: 번역 값은 비어 있지 않은 문자열이어야 합니다.`);
    }
    return result;
}

function variables(value: string): string {
    return [
        ...new Set(
            [...value.matchAll(/{{\s*-?\s*([^,}]+?)(?:,[^}]*)?\s*}}/g)].map((match) =>
                match[1].trim(),
            ),
        ),
    ]
        .sort()
        .join(",");
}

const ko = flatten(
    JSON.parse(await readFile(new URL("../src/locales/ko.json", import.meta.url), "utf8")),
);
const en = flatten(
    JSON.parse(await readFile(new URL("../src/locales/en.json", import.meta.url), "utf8")),
);
for (const key of new Set([...ko.keys(), ...en.keys()])) {
    const korean = ko.get(key);
    const english = en.get(key);
    if (korean === undefined || english === undefined) {
        errors.push(`${key}: ${korean === undefined ? "한국어" : "영어"} 번역 키가 없습니다.`);
    } else if (variables(korean) !== variables(english)) {
        errors.push(`${key}: 한국어·영어 치환 변수 이름이 다릅니다.`);
    }
}
if (errors.length) {
    process.stderr.write(errors.join("\n") + "\n");
    process.exitCode = 1;
} else {
    process.stdout.write(`번역 ${ko.size}개 키·치환 변수 일치 검사 통과\n`);
}
