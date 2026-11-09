import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";
import { invalidateOtherTabs } from "./cookieCoordination.ts";
import {
    clearAccessToken,
    getAccessTokenSnapshot,
    getAuthRevision,
    replaceAccessToken,
    type AccessTokenSnapshot,
} from "./tokenStore.ts";

let pending: { revision: number; promise: Promise<AccessTokenSnapshot> } | undefined;

export function refreshAccessToken(
    issue: () => Promise<components["schemas"]["LoginResponse"]>,
    revision = getAuthRevision(),
): Promise<AccessTokenSnapshot> {
    if (revision !== getAuthRevision()) return Promise.reject(new ApiError("aborted"));
    if (pending?.revision === revision) return pending.promise;

    const promise = (async () => {
        try {
            const response = await issue();
            if (revision !== getAuthRevision()) throw new ApiError("aborted");
            replaceAccessToken(response);
            const snapshot = getAccessTokenSnapshot();
            if (!snapshot) throw new ApiError("authentication-required");
            return snapshot;
        } catch (error) {
            // 새 로그인이나 로그아웃 이후의 늦은 갱신은 현재 상태에 손대지 않는다.
            if (revision === getAuthRevision()) {
                clearAccessToken(error);
                invalidateOtherTabs();
            }
            throw error;
        } finally {
            if (pending?.revision === revision) pending = undefined;
        }
    })();
    pending = { revision, promise };
    return promise;
}
