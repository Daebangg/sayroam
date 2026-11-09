import assert from "node:assert/strict";
import { test } from "node:test";
import { isMemberPath, legacyPageUrl } from "../src/routing/paths.ts";

test("여행·내 방만 보호하고 라우터와 동일하게 대소문자·끝 슬래시를 처리", () => {
    for (const path of ["/trips", "/rooms", "/TRIPS/", "/Rooms///"])
        assert.equal(isMemberPath(path), true);
    for (const path of ["/", "/matching", "/brand", "/rooms/unknown", "//external.test/trips"])
        assert.equal(isMemberPath(path), false);
});

test("기존 화면 미리보기 URL을 동일한 화면 경로로 바꾸고 설정·해시 유지", () => {
    for (const page of ["trips", "matching", "rooms", "brand"]) {
        const url = new URL(`https://sayroam.test/?view=${page}&lang=en&theme=dark#section`);
        assert.equal(legacyPageUrl(url), `/${page}?lang=en&theme=dark#section`);
        assert.equal(url.pathname, "/");
    }
});

test("실제 경로를 우선하고 지원하지 않는 미리보기 값은 주소로 사용하지 않음", () => {
    for (const path of [
        "/trips?view=brand",
        "/?view=https://example.com",
        "/?view=__proto__",
        "/",
    ]) {
        assert.equal(legacyPageUrl(new URL(path, "https://sayroam.test")), null);
    }
});
