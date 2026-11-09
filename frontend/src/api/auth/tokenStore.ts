import type { components } from "../generated/schema.ts";

type LoginResponse = components["schemas"]["LoginResponse"];
export type AccessTokenSnapshot = Readonly<{
    value: string;
    expiresAt: number;
    revision: number;
}>;

let revision = 0;
let snapshot: AccessTokenSnapshot | undefined;
const invalidationListeners = new Set<(cause?: unknown) => void>();

export function subscribeAuthInvalidation(listener: (cause?: unknown) => void): () => void {
    invalidationListeners.add(listener);
    return () => {
        invalidationListeners.delete(listener);
    };
}

export function getAuthRevision(): number {
    return revision;
}

export function getAccessToken(): string | undefined {
    return snapshot?.value;
}

export function getAccessTokenSnapshot(): AccessTokenSnapshot | undefined {
    return snapshot;
}

export function setAccessToken(response: LoginResponse): void {
    revision++;
    replaceAccessToken(response);
}

// 같은 로그인에서의 갱신은 계정 변경이 아니다. 진행 중 요청의 인증 버전을 유지한다.
export function replaceAccessToken(response: LoginResponse): void {
    snapshot = Object.freeze({
        value: response.accessToken,
        expiresAt: Date.now() + response.expiresIn * 1000,
        revision,
    });
}

// 메모리 정리만 수행한다. 서버 Refresh 쿠키를 폐기하는 로그아웃은 별도 작업이다.
export function clearAccessToken(cause?: unknown): void {
    revision++;
    snapshot = undefined;
    invalidationListeners.forEach((listener) => listener(cause));
}
