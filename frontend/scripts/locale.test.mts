import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { copyFileSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { formatDateRange, formatNumber, normalizeLanguage } from "../src/i18n/locale.ts";

test("지원하지 않는 언어와 저장 값은 한국어로 대체", () => {
    for (const value of [null, undefined, "fr", "en-US", "", {}, 1]) {
        assert.equal(normalizeLanguage(value), "ko");
    }
    assert.equal(normalizeLanguage("en"), "en");
});

test("날짜만 있는 여행 일정은 브라우저 시간대와 무관하게 같은 날짜 표시", () => {
    const previous = process.env.TZ;
    try {
        process.env.TZ = "America/Los_Angeles";
        const west = formatDateRange("en", "2026-02-10", "2026-02-13");
        process.env.TZ = "Asia/Seoul";
        assert.equal(formatDateRange("en", "2026-02-10", "2026-02-13"), west);
        assert.match(west, /Feb/);
        assert.match(west, /10/);
        assert.match(west, /13/);
        assert.match(west, /2026/);
        assert.notEqual(formatDateRange("ko", "2026-02-10", "2026-02-13"), west);
    } finally {
        if (previous === undefined) delete process.env.TZ;
        else process.env.TZ = previous;
    }
});

test("인원과 소수 숫자에 언어별 숫자 형식 적용", () => {
    assert.equal(formatNumber("ko", 1234567.89), "1,234,567.89");
    assert.equal(formatNumber("en", 1234567.89), "1,234,567.89");
    assert.equal(formatNumber("ko", 2), "2");
});

test("번역 검사는 하이픈 변수의 불일치를 차단하고 형식 지정·이스케이프 차이는 허용", () => {
    const directory = mkdtempSync(join(tmpdir(), "sayroam-i18n-check-"));
    try {
        mkdirSync(join(directory, "scripts"));
        mkdirSync(join(directory, "src/locales"), { recursive: true });
        const script = join(directory, "scripts/i18n-check.mts");
        copyFileSync(new URL("./i18n-check.mts", import.meta.url), script);
        for (const [korean, english, expected] of [
            ["{{user-name}}", "{{other-name}}", 1],
            ["{{user-name , number}}", "{{- user-name, currency}}", 0],
            ["{{ user-name }}", "{{user-name}}", 0],
            ["{{사용자-이름}}", "{{다른-이름}}", 1],
        ] as const) {
            writeFileSync(
                join(directory, "src/locales/ko.json"),
                JSON.stringify({ message: korean }),
            );
            writeFileSync(
                join(directory, "src/locales/en.json"),
                JSON.stringify({ message: english }),
            );
            const result = spawnSync(process.execPath, [script], {
                encoding: "utf8",
                timeout: 5000,
            });
            assert.equal(result.status, expected, result.stderr);
            if (expected === 1) assert.match(result.stderr, /message:.*치환 변수/);
        }
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
});
