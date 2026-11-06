import type { components } from "../generated/schema.ts";
import type { RequestOptions } from "../http.ts";
import { login, logout, refresh } from "../sdk.ts";
import { validateToken } from "./tokenResponse.ts";

export type LoginRequest = components["schemas"]["LoginRequest"];
export type LoginResponse = components["schemas"]["LoginResponse"];
export type AuthCallOptions = Pick<RequestOptions, "signal">;

export async function loginApi(
    input: LoginRequest,
    options: AuthCallOptions = {},
): Promise<LoginResponse> {
    const value = await login({
        ...options,
        body: input,
    });
    return validateToken(value);
}

export async function refreshApi(): Promise<LoginResponse> {
    // 공유 갱신에는 개별 화면의 AbortSignal을 연결하지 않는다.
    return validateToken(await refresh());
}

export async function logoutApi(): Promise<void> {
    await logout();
}
