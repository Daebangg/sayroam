import { ApiError } from "./apiError.ts";

// 공유 작업 자체는 취소하지 않고 해당 호출의 대기만 끝낸다.
export function waitForSharedRequest<T>(
    pending: Promise<T>,
    signal: AbortSignal | null | undefined,
): Promise<T> {
    if (!signal) return pending;

    return new Promise<T>((resolve, reject) => {
        const aborted = (): void => {
            signal.removeEventListener("abort", aborted);
            reject(new ApiError("aborted", { cause: signal.reason }));
        };
        pending.then(
            (value) => {
                signal.removeEventListener("abort", aborted);
                resolve(value);
            },
            (error: unknown) => {
                signal.removeEventListener("abort", aborted);
                reject(error);
            },
        );
        if (signal.aborted) aborted();
        else signal.addEventListener("abort", aborted, { once: true });
    });
}
