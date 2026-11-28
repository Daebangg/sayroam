import assert from "node:assert/strict";
import { test } from "node:test";
import { runOneAtATime } from "../src/hooks/api/chat/runOneAtATime.ts";
import { createReadPositionSender } from "../src/hooks/api/chat/readPositionSender.ts";
import { createSummaryLoader, type ChatSummary } from "../src/hooks/api/chat/summaryLoader.ts";
import { latestMessageId, unreadCount } from "../src/hooks/api/chat/unreadCount.ts";
import { parseRoomNotice, type ChatMessage } from "../src/api/chat/chatResponse.ts";

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

function fakeClock() {
    let now = 0;
    let nextId = 1;
    const timers = new Map<number, { at: number; run: () => void }>();
    return {
        timers: {
            now: () => now,
            setTimeout(run: () => void, ms: number): number {
                const id = nextId++;
                timers.set(id, { at: now + ms, run });
                return id;
            },
            clearTimeout(id: number): void {
                timers.delete(id);
            },
        },
        advance(ms: number) {
            now += ms;
            for (const [id, timer] of [...timers].sort((a, b) => a[1].at - b[1].at)) {
                if (timer.at > now) continue;
                timers.delete(id);
                timer.run();
            }
        },
    };
}

function recordingSender(clock: ReturnType<typeof fakeClock>, fail = false) {
    const sent: number[] = [];
    const sender = createReadPositionSender(
        (id) => {
            sent.push(id);
            return fail ? Promise.reject(new Error("failed")) : Promise.resolve();
        },
        { idleMs: 1000, maxWaitMs: 5000 },
        clock.timers,
    );
    return { sent, sender };
}

test("연달아 표시하면 조용해진 뒤 가장 큰 ID만 한 번 보냄", () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock);

    sender.mark(10);
    clock.advance(500);
    sender.mark(11);
    clock.advance(500);
    sender.mark(12);
    clock.advance(999);
    assert.deepEqual(sent, []);
    clock.advance(1);

    assert.deepEqual(sent, [12]);
});

test("계속 표시돼도 최대 대기 시간 안에는 보냄", () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock);

    for (let id = 1; id <= 10; id += 1) {
        sender.mark(id);
        clock.advance(600);
    }

    assert.deepEqual(sent.slice(0, 1), [9]);
});

test("flush하면 기다리지 않고 보내고, 보낼 것이 없으면 보내지 않음", () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock);

    sender.flush();
    sender.mark(7);
    sender.flush();
    clock.advance(5000);

    assert.deepEqual(sent, [7]);
});

test("이미 보낸 ID 이하는 다시 보내지 않음", () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock);

    sender.mark(7);
    sender.flush();
    sender.mark(7);
    sender.mark(5);
    sender.flush();

    assert.deepEqual(sent, [7]);
});

test("보내기에 실패하면 같은 ID를 다시 표시할 때 다시 보냄", async () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock, true);

    sender.mark(7);
    sender.flush();
    await Promise.resolve();
    await Promise.resolve();
    sender.mark(7);
    sender.flush();

    assert.deepEqual(sent, [7, 7]);
});

test("정리하면 기다리던 전송을 취소함", () => {
    const clock = fakeClock();
    const { sent, sender } = recordingSender(clock);

    sender.mark(7);
    sender.dispose();
    clock.advance(5000);

    assert.deepEqual(sent, []);
});

test("서버 기준 ID 이하로 응답 전에 도착한 메시지는 다시 더하지 않음", () => {
    const messages = [message(9, OTHER), message(10, OTHER), message(11, OTHER)];

    assert.equal(unreadCount(messages, ME, { count: 2, until: 10 }, null), 3);
});

test("서버 기준 이후 도착한 메시지가 없으면 서버 개수를 그대로 보여 줌", () => {
    const messages = [message(9, OTHER), message(10, OTHER)];

    assert.equal(unreadCount(messages, ME, { count: 2, until: 10 }, null), 2);
});

test("보내기에 실패하면 새로 표시하지 않아도 다음 flush 때 다시 보냄", async () => {
    const clock = fakeClock();
    let fail = true;
    const sent: number[] = [];
    const sender = createReadPositionSender(
        (id) => {
            sent.push(id);
            return fail ? Promise.reject(new Error("failed")) : Promise.resolve();
        },
        { idleMs: 1000, maxWaitMs: 5000 },
        clock.timers,
    );

    sender.mark(7);
    sender.flush();
    await Promise.resolve();
    await Promise.resolve();
    fail = false;
    sender.flush();
    sender.flush();

    assert.deepEqual(sent, [7, 7]);
});

function deferredSummary() {
    let resolve!: (summary: ChatSummary) => void;
    let reject!: (error: unknown) => void;
    const promise = new Promise<ChatSummary>((done, fail) => {
        resolve = done;
        reject = fail;
    });
    return { promise, resolve, reject };
}

async function settle() {
    for (let i = 0; i < 5; i += 1) await Promise.resolve();
}

test("요약 조회가 실패하면 다음 요청 때 다시 조회하고, 성공한 뒤에는 다시 조회하지 않음", async () => {
    const calls: ReturnType<typeof deferredSummary>[] = [];
    const loaded: ChatSummary[] = [];
    const loader = createSummaryLoader(
        () => {
            const call = deferredSummary();
            calls.push(call);
            return call.promise;
        },
        (summary) => loaded.push(summary),
    );

    loader.request();
    calls[0].reject(new Error("failed"));
    await settle();
    loader.request();
    calls[1].resolve({ unreadCount: 3, lastMessageId: 10 });
    await settle();
    loader.request();

    assert.equal(calls.length, 2);
    assert.deepEqual(loaded, [{ unreadCount: 3, lastMessageId: 10 }]);
    assert.equal(
        unreadCount([message(10, OTHER), message(11, OTHER)], ME, { count: 3, until: 10 }, null),
        4,
    );
});

test("요약 조회 중에 다시 요청해도 한 번만 조회", () => {
    let calls = 0;
    const loader = createSummaryLoader(
        () => {
            calls += 1;
            return new Promise<ChatSummary>(() => undefined);
        },
        () => undefined,
    );

    loader.request();
    loader.request();

    assert.equal(calls, 1);
});

test("정리한 뒤 도착한 요약은 반영하지 않음", async () => {
    const call = deferredSummary();
    const loaded: ChatSummary[] = [];
    const loader = createSummaryLoader(
        () => call.promise,
        (summary) => loaded.push(summary),
    );

    loader.request();
    loader.dispose();
    call.resolve({ unreadCount: 3, lastMessageId: 10 });
    await settle();

    assert.deepEqual(loaded, []);
});

test("방 나감 알림은 type과 방 ID를 읽음", () => {
    assert.deepEqual(parseRoomNotice('{"type":"LEFT","roomId":7}'), { type: "LEFT", roomId: 7 });
});

test("모르는 type이나 형식이 맞지 않는 방 알림은 버림", () => {
    assert.equal(parseRoomNotice('{"type":"JOINED","roomId":7}'), null);
    assert.equal(parseRoomNotice('{"type":"LEFT","roomId":0}'), null);
    assert.equal(parseRoomNotice('{"type":"LEFT"}'), null);
    assert.equal(parseRoomNotice("not json"), null);
});
