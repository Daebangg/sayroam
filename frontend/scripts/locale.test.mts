import assert from "node:assert/strict";
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
