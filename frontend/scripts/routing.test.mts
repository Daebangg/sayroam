import assert from "node:assert/strict";
import { test } from "node:test";
import { isMemberPath, legacyPageUrl } from "../src/routing/paths.ts";

test("여행·내 방·계정·프로필은 보호하고 대소문자·끝 슬래시를 처리", () => {
    for (const path of [
        "/trips",
        "/rooms",
        "/TRIPS/",
        "/Rooms///",
        "/settings/account",
        "/Settings/Profile/",
    ])
        assert.equal(isMemberPath(path), true);
    for (const path of [
        "/",
        "/matching",
        "/brand",
        "/rooms/unknown",
        "//external.test/trips",
        "/settings",
        "/settings/appearance",
        "/settings/account/unknown",
    ])
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

test("목업 화면은 /brand의 지정된 preview 값에서만 열림", async () => {
    const { getMockupScreen, mockupScreens, mockupUrl } = await import("../src/routing/mockups.ts");
    for (const screen of mockupScreens) {
        const url = new URL(mockupUrl(screen), "https://sayroam.test");
        assert.equal(getMockupScreen(url.pathname, url.search), screen);
        assert.equal(getMockupScreen("/BRAND///", url.search), screen);
    }
    for (const search of [
        "",
        "?preview=unknown",
        "?preview=https://example.com",
        "?preview=__proto__",
    ])
        assert.equal(getMockupScreen("/brand", search), null);
    for (const path of ["/", "/trips", "/rooms", "/brand/unknown", "//brand"])
        assert.equal(getMockupScreen(path, "?preview=home"), null);
});

test("목업 쿼리로 실제 회원 전용 경로를 공개 화면으로 바꾸지 않음", async () => {
    const { getMockupScreen } = await import("../src/routing/mockups.ts");
    for (const path of [
        "/trips",
        "/rooms",
        "/TRIPS/",
        "/Rooms///",
        "/settings/account",
        "/settings/profile",
    ]) {
        assert.equal(isMemberPath(path), true);
        assert.equal(getMockupScreen(path, "?preview=home"), null);
    }
});
