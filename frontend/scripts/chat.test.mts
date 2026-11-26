import assert from "node:assert/strict";
import { test } from "node:test";
import { runOneAtATime } from "../src/hooks/api/chat/runOneAtATime.ts";

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
