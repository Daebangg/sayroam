import assert from "node:assert/strict";
import { test } from "node:test";
import { runOneAtATime } from "../src/hooks/api/chat/runOneAtATime.ts";
import { latestMessageId, unreadCount } from "../src/hooks/api/chat/unreadCount.ts";
import type { ChatMessage } from "../src/api/chat/chatResponse.ts";

function deferred() {
    let resolve!: () => void;
    let reject!: (error: unknown) => void;
    const promise = new Promise<void>((done, fail) => {
        resolve = done;
        reject = fail;
    });
    return { promise, resolve, reject };
}

test("도는 중에 다시 요청되면 끝난 뒤 한 번만 더 실행", async () => {
    const first = deferred();
    let calls = 0;
    const run = runOneAtATime(async () => {
        calls += 1;
        if (calls === 1) await first.promise;
    }, new AbortController().signal);

    const running = run();
    void run();
    void run();
    first.resolve();
    await running;

    assert.equal(calls, 2);
});

test("현재 실행이 실패해도 보류된 요청은 다시 실행", async () => {
    const first = deferred();
    let calls = 0;
    const run = runOneAtATime(async () => {
        calls += 1;
        if (calls === 1) await first.promise;
    }, new AbortController().signal);

    const running = run();
    void run();
    first.reject(new Error("network"));
    await running;

    assert.equal(calls, 2);
});

test("보류된 요청 없이 실패하면 호출자에게 오류 전달", async () => {
    const run = runOneAtATime(async () => {
        throw new Error("network");
    }, new AbortController().signal);

    await assert.rejects(run(), /network/);
});

test("마지막 보류 실행까지 실패하면 그 오류를 전달", async () => {
    const first = deferred();
    let calls = 0;
    const run = runOneAtATime(async () => {
        calls += 1;
        if (calls === 1) await first.promise;
        else throw new Error("still down");
    }, new AbortController().signal);

    const running = run();
    void run().catch(() => undefined);
    first.reject(new Error("network"));

    await assert.rejects(running, /still down/);
    assert.equal(calls, 2);
});

test("취소되면 보류된 요청을 실행하지 않음", async () => {
    const first = deferred();
    const controller = new AbortController();
    let calls = 0;
    const run = runOneAtATime(async () => {
        calls += 1;
        await first.promise;
    }, controller.signal);

    const running = run();
    void run();
    controller.abort();
    first.resolve();
    await running;

    assert.equal(calls, 1);
});

const ME = 1;
const OTHER = 2;

function message(messageId: number, senderId: number | null): ChatMessage {
    return {
        messageId,
        roomId: 7,
        senderId,
        clientMessageId: null,
        content: "안녕",
        createdAt: "2026-11-27T00:00:00Z",
    };
}

test("서버 개수에 기준 이후 도착한 다른 회원의 메시지를 더함", () => {
    const messages = [message(10, OTHER), message(11, OTHER), message(12, ME)];

    assert.equal(unreadCount(messages, ME, { count: 3, until: 10 }, null), 4);
});

test("서버 개수를 받기 전에는 0", () => {
    assert.equal(unreadCount([message(10, OTHER)], ME, null, null), 0);
});

test("패널을 닫은 뒤에는 본 위치 이후 다른 회원의 메시지만 셈", () => {
    const messages = [message(10, OTHER), message(11, ME), message(12, OTHER)];

    assert.equal(unreadCount(messages, ME, { count: 5, until: 9 }, 10), 1);
});

test("가장 뒤 메시지 ID를 읽은 위치로 사용하고 메시지가 없으면 0", () => {
    assert.equal(latestMessageId([message(10, OTHER), message(12, ME)]), 12);
    assert.equal(latestMessageId([]), 0);
});
