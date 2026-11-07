import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { runInNewContext } from "node:vm";
import type { ThemeController } from "../src/hooks/theme.types.ts";

const source = readFileSync(new URL("../public/assets/theme.js", import.meta.url), "utf8");
const key = "sayroam.theme.v1";

function browser(
    options: { saved?: string; dark?: boolean; blocked?: boolean; query?: string } = {},
) {
    const storage = new Map(options.saved === undefined ? [] : [[key, options.saved]]);
    const changes = new Set<() => void>();
    const events = new Set<(event: { key: string | null }) => void>();
    const media = {
        matches: options.dark ?? false,
        addEventListener: (_type: string, callback: () => void) => changes.add(callback),
    };
    const root = { dataset: {} as Record<string, string> };
    const window = {
        location: new URL(`https://sayroam.test/${options.query ?? ""}`),
        history: {
            state: { preview: true },
            replaceState: (_state: unknown, _title: string, url: URL) => {
                window.location = url;
            },
        },
        matchMedia: () => media,
        get localStorage() {
            if (options.blocked) throw new Error("storage blocked");
            return {
                getItem: (name: string) => storage.get(name) ?? null,
                setItem: (name: string, value: string) => storage.set(name, value),
            };
        },
        addEventListener: (_type: string, callback: (event: { key: string | null }) => void) =>
            events.add(callback),
    };
    runInNewContext(source, { window, document: { documentElement: root }, URL, URLSearchParams });
    const controller = Reflect.get(window, "sayroamTheme") as ThemeController;
    return {
        controller,
        root,
        storage,
        window,
        system(dark: boolean) {
            media.matches = dark;
            changes.forEach((change) => change());
        },
        stored(value?: string) {
            if (value === undefined) storage.delete(key);
            else storage.set(key, value);
            events.forEach((event) => event({ key: value === undefined ? null : key }));
        },
    };
}

test("저장된 선택이 없으면 첫 화면부터 시스템 테마 적용", () => {
    const app = browser({ dark: true });
    assert.equal(app.root.dataset.theme, "dark");
    assert.equal(app.controller.getSnapshot().preference, "system");
    app.system(false);
    assert.equal(app.root.dataset.theme, "light");
});

test("명시적 테마는 시스템 변경보다 우선하고 재실행 후 복원", () => {
    const app = browser({ saved: "light", dark: true });
    app.system(false);
    app.system(true);
    assert.equal(app.root.dataset.theme, "light");
    app.controller.setPreference("dark");
    assert.equal(browser({ saved: app.storage.get(key)! }).root.dataset.theme, "dark");
});

test("시스템으로 돌아가면 현재 시스템 설정과 이후 변경 추적", () => {
    const app = browser({ saved: "light", dark: true });
    app.controller.setPreference("system");
    assert.equal(app.root.dataset.theme, "dark");
    app.system(false);
    assert.equal(app.root.dataset.theme, "light");
    assert.equal(app.storage.get(key), "system");
});

test("저장소 접근이 차단돼도 화면 선택과 시스템 연동 동작", () => {
    const app = browser({ blocked: true });
    app.controller.setPreference("dark");
    assert.equal(app.root.dataset.theme, "dark");
    app.controller.setPreference("system");
    app.system(true);
    assert.equal(app.root.dataset.theme, "dark");
});

test("손상된 저장 값은 시스템으로 복구하고 다른 탭 변경·삭제 반영", () => {
    const app = browser({ saved: "unsupported", dark: true });
    assert.equal(app.controller.getSnapshot().preference, "system");
    app.stored("light");
    assert.equal(app.root.dataset.theme, "light");
    app.stored();
    assert.equal(app.root.dataset.theme, "dark");
});

test("미리보기 URL은 저장하지 않고 UI 선택 시 해당 매개변수만 제거", () => {
    const app = browser({ saved: "light", query: "?view=brand&theme=dark&lang=en" });
    assert.equal(app.root.dataset.theme, "dark");
    assert.equal(app.storage.get(key), "light");
    app.controller.setPreference("system");
    assert.equal(app.window.location.search, "?view=brand&lang=en");
});

test("테마 미리보기 중에는 다른 탭의 저장 변경·삭제보다 URL이 우선", () => {
    const app = browser({ saved: "light", query: "?theme=dark" });
    app.stored("light");
    assert.equal(app.root.dataset.theme, "dark");
    app.stored();
    assert.equal(app.root.dataset.theme, "dark");
});

test("UI 선택으로 미리보기를 해제하면 다른 탭의 변경·삭제를 다시 반영", () => {
    const app = browser({ saved: "light", query: "?theme=dark&lang=en" });
    app.controller.setPreference("light");
    assert.equal(app.window.location.search, "?lang=en");
    app.stored("dark");
    assert.equal(app.root.dataset.theme, "dark");
    app.stored();
    assert.equal(app.controller.getSnapshot().preference, "system");
    assert.equal(app.root.dataset.theme, "light");
});

test("동일 상태의 snapshot을 유지하고 구독 해제 후 알림 중단", () => {
    const app = browser();
    const initial = app.controller.getSnapshot();
    let notifications = 0;
    const unsubscribe = app.controller.subscribe(() => notifications++);
    app.system(false);
    assert.equal(app.controller.getSnapshot(), initial);
    app.controller.setPreference("dark");
    assert.equal(notifications, 1);
    unsubscribe();
    app.controller.setPreference("light");
    assert.equal(notifications, 1);
});
