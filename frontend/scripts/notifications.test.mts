/// <reference lib="dom" />
import assert from "node:assert/strict";
import { afterEach, beforeEach, test, type TestContext } from "node:test";
import { setImmediate } from "node:timers/promises";
import { clearAccessToken, setAccessToken } from "../src/api/auth/tokenStore.ts";
import { ApiError } from "../src/api/apiError.ts";
import { createNotificationController } from "../src/hooks/api/notifications/notificationController.ts";
import {
    validateNotificationList,
    validateNotificationSummary,
} from "../src/api/notifications/notificationResponse.ts";
import type { Notification } from "../src/hooks/notification.types.ts";

const notification = (id: number, isRead = false): Notification => ({
    id,
    isRead,
    type: "WELCOME",
    content: "Welcome to SayRoam!",
    targetId: null,
    targetType: null,
    createdAt: "2026-11-21T08:00:00Z",
});
const settle = async (): Promise<void> => {
    await setImmediate();
    await setImmediate();
};

function server(t: TestContext, initial = [notification(1)]) {
    let records = initial;
    let fail = false;
    const calls: { url: URL; method: string; body: unknown }[] = [];
    t.mock.method(globalThis, "fetch", async (path: unknown, options?: RequestInit) => {
        const url = new URL(String(path), "http://localhost");
        const method = options?.method ?? "GET";
        const body = options?.body ? JSON.parse(String(options.body)) : null;
        calls.push({ url, method, body });
        if (fail) throw new TypeError("offline");
        if (method !== "GET") {
            const id = Number(url.pathname.split("/").at(-1));
            if (method === "DELETE") records = records.filter((item) => item.id !== id);
            else if (url.pathname.endsWith("/notifications"))
                records = records.map((item) => ({ ...item, isRead: true }));
            else
                records = records.map((item) =>
                    item.id === id ? { ...item, isRead: body.isRead } : item,
                );
            return new Response(null, { status: 204 });
        }
        if (url.pathname.endsWith("/summary"))
            return Response.json({
                totalCount: records.length,
                unreadCount: records.filter((item) => !item.isRead).length,
            });
        const beforeId = Number(url.searchParams.get("beforeId") ?? Infinity);
        const unreadOnly = url.searchParams.get("unreadOnly") === "true";
        return Response.json({
            notifications: records
                .filter((item) => item.id < beforeId && (!unreadOnly || !item.isRead))
                .slice(0, 20),
        });
    });
    t.mock.timers.enable({ apis: ["setTimeout", "Date"], now: 10_000 });
    setAccessToken({ accessToken: "notification-token", expiresIn: 3600, tokenType: "Bearer" });
    const controller = createNotificationController();
    t.after(() => controller.disconnect());
    return {
        controller,
        calls,
        replace(value: Notification[]): void {
            records = value;
        },
        fail(value: boolean): void {
            fail = value;
        },
    };
}

beforeEach(() => clearAccessToken());
afterEach(() => clearAccessToken());

test("로그인 최초 기준을 저장하고 닫힘 60초·열림 30초·닫힘 60초로 전환", async (t) => {
    const { controller, calls } = server(t);
    controller.connect(true);
    await settle();
    assert.equal(calls.length, 2);
    assert.equal(controller.getSnapshot().arrivalVersion, 0);
    t.mock.timers.tick(59_999);
    await settle();
    assert.equal(calls.length, 2);
    t.mock.timers.tick(1);
    await settle();
    assert.equal(calls.length, 3);
    assert(calls.at(-1)?.url.pathname.endsWith("/summary"));
    controller.setOpen(true);
    await settle();
    assert.equal(calls.length, 5);
    t.mock.timers.tick(30_000);
    await settle();
    assert.equal(calls.length, 7);
    controller.setOpen(false);
    t.mock.timers.tick(30_000);
    await settle();
    assert.equal(calls.length, 7);
    t.mock.timers.tick(30_000);
    await settle();
    assert.equal(calls.length, 8);
});

test("새 ID의 미읽음 알림만 도착으로 판별하고 읽지 않음 전환에는 반응하지 않음", async (t) => {
    const { controller, replace } = server(t, [notification(3), notification(2, true)]);
    controller.connect(true);
    await settle();
    await controller.setRead(2, false);
    await settle();
    assert.equal(controller.getSnapshot().arrivalVersion, 0);
    replace([notification(4), notification(3), notification(2)]);
    t.mock.timers.tick(60_000);
    await settle();
    assert.equal(controller.getSnapshot().arrivalVersion, 1);
    assert.equal(controller.getSnapshot().summary?.unreadCount, 3);
});

test("숨긴 탭·오프라인에서는 중단하고 복귀 시 갱신, 짧은 포커스 반복은 중복 조회하지 않음", async (t) => {
    const { controller, calls } = server(t);
    controller.connect(true);
    await settle();
    controller.setAvailable(false);
    t.mock.timers.tick(180_000);
    await settle();
    assert.equal(calls.length, 2);
    controller.setAvailable(true);
    await settle();
    assert.equal(calls.length, 3);
    controller.setAvailable(true);
    await settle();
    assert.equal(calls.length, 3);
    controller.disconnect();
    t.mock.timers.tick(180_000);
    await settle();
    assert.equal(calls.length, 3);
    assert.equal(controller.getSnapshot().summary, null);
});

test("실패 후 120초·240초·300초로 늘리고 성공 후 60초로 복귀", async (t) => {
    const { controller, calls, fail } = server(t);
    fail(true);
    controller.connect(true);
    await settle();
    assert.equal(calls.length, 1);
    t.mock.timers.tick(119_999);
    await settle();
    assert.equal(calls.length, 1);
    t.mock.timers.tick(1);
    await settle();
    assert.equal(calls.length, 2);
    t.mock.timers.tick(239_999);
    await settle();
    assert.equal(calls.length, 2);
    t.mock.timers.tick(1);
    await settle();
    assert.equal(calls.length, 3);
    fail(false);
    t.mock.timers.tick(300_000);
    await settle();
    assert.equal(calls.length, 5);
    assert.equal(controller.getSnapshot().error, null);
    t.mock.timers.tick(60_000);
    await settle();
    assert.equal(calls.length, 6);
});

test("오프라인 첫 진입은 빈 알림으로 오인하지 않고 복귀 후 정상 조회", async (t) => {
    const { controller, calls } = server(t);
    controller.connect(false);
    controller.setOpen(true);
    await settle();
    assert.equal(calls.length, 0);
    assert.equal(controller.getSnapshot().listLoaded, false);
    assert.equal(controller.getSnapshot().error, "load");
    controller.setAvailable(true);
    await settle();
    assert.equal(controller.getSnapshot().listLoaded, true);
    assert.equal(controller.getSnapshot().error, null);
});

test("느린 요청 중 타이머·열기·갱신이 겹쳐도 조회를 동시에 시작하지 않음", async (t) => {
    const { controller, calls } = server(t);
    controller.connect(true);
    await settle();
    let release!: () => void;
    const waiting = new Promise<void>((resolve) => {
        release = resolve;
    });
    let count = 0;
    t.mock.method(globalThis, "fetch", async () => {
        count++;
        await waiting;
        return Response.json({ totalCount: 1, unreadCount: 1 });
    });
    t.mock.timers.tick(60_000);
    await settle();
    controller.reload();
    t.mock.timers.tick(300_000);
    await settle();
    assert.equal(count, 1);
    assert.equal(calls.length, 2);
    controller.disconnect();
    release();
    await settle();
    assert.equal(controller.getSnapshot().summary, null);
});

test("변경은 한 번 전송하고 실패 후 조회로 확인하며 중복 클릭을 차단", async (t) => {
    const { controller, calls, fail } = server(t);
    controller.connect(true);
    await settle();
    await controller.setRead(1, true);
    await settle();
    assert.equal(controller.getSnapshot().summary?.unreadCount, 0);
    assert.deepEqual(calls.find((call) => call.method === "PATCH")?.body, { isRead: true });
    fail(true);
    await Promise.all([controller.delete(1), controller.delete(1)]);
    await settle();
    assert.equal(calls.filter((call) => call.method === "DELETE").length, 1);
    assert.equal(controller.getSnapshot().error, "change");
    t.mock.timers.tick(120_000);
    await settle();
    assert.equal(calls.filter((call) => call.method === "DELETE").length, 1);
});

test("커서 더 보기·서버 미읽음 필터·전체 읽음·삭제를 실제 API 계약으로 처리", async (t) => {
    const records = Array.from({ length: 25 }, (_, i) => notification(25 - i, i % 2 === 0));
    const { controller, calls } = server(t, records);
    controller.connect(true);
    await settle();
    controller.setOpen(true);
    await settle();
    assert.equal(controller.getSnapshot().notifications.length, 20);
    await controller.loadMore();
    assert.equal(controller.getSnapshot().notifications.length, 25);
    assert.equal(calls.at(-1)?.url.searchParams.get("beforeId"), "6");
    assert.equal(controller.getSnapshot().arrivalVersion, 0);
    controller.setUnreadOnly(true);
    await settle();
    assert.equal(controller.getSnapshot().notifications.length, 12);
    assert(calls.at(-1)?.url.searchParams.get("unreadOnly") === "true");
    await controller.readAll();
    await settle();
    assert.equal(controller.getSnapshot().summary?.unreadCount, 0);
    assert.deepEqual(calls.find((call) => call.method === "PATCH")?.body, { isRead: true });
    await controller.delete(24);
    await settle();
    assert.equal(controller.getSnapshot().summary?.totalCount, 24);
});

test("인증 무효화 후 늦은 조회 응답이 다른 계정 상태를 복원하지 않음", async (t) => {
    const { controller } = server(t);
    let release!: (response: Response) => void;
    t.mock.method(
        globalThis,
        "fetch",
        () =>
            new Promise<Response>((resolve) => {
                release = resolve;
            }),
    );
    controller.connect(true);
    await settle();
    clearAccessToken();
    setAccessToken({ accessToken: "other-account", tokenType: "Bearer", expiresIn: 3600 });
    release(Response.json({ totalCount: 99, unreadCount: 99 }));
    await settle();
    assert.equal(controller.getSnapshot().summary, null);
    assert.deepEqual(controller.getSnapshot().notifications, []);
});

test("잘못된 알림 ID·정렬·개수·날짜 응답을 화면 상태에 넣지 않음", () => {
    const invalid = (error: unknown): boolean =>
        error instanceof ApiError && error.kind === "invalid-response";
    for (const records of [
        [notification(1), notification(2)],
        [notification(1), notification(1)],
        [{ ...notification(1), id: 1.5 }],
        [{ ...notification(1), createdAt: "invalid" }],
    ]) {
        assert.throws(() => validateNotificationList({ notifications: records }, 200), invalid);
    }
    assert.throws(
        () => validateNotificationSummary({ totalCount: 1, unreadCount: 2 }, 200),
        invalid,
    );
    assert.throws(
        () => validateNotificationSummary({ totalCount: -1, unreadCount: 0 }, 200),
        invalid,
    );
});
