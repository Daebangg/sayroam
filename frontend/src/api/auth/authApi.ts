import { api } from "../api.ts";
import type { ApiCallOptions, ApiResponse } from "../router.ts";

export type LoginRequest = ApiCallOptions<"login">["body"];
export type LoginResponse = ApiResponse<"login">;
export type AuthCallOptions = Pick<ApiCallOptions<"login">, "signal">;

export function loginApi(
    input: LoginRequest,
    options: AuthCallOptions = {},
): Promise<LoginResponse> {
    return api.call("login", { ...options, body: input });
}

export function refreshApi(): Promise<LoginResponse> {
    // 공유 갱신에는 개별 화면의 AbortSignal을 연결하지 않는다.
    return api.call("refresh");
}

export async function logoutApi(): Promise<void> {
    await api.call("logout");
}
