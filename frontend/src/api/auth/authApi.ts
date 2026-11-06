import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";
import { request, type RequestOptions } from "../http.ts";
import { validateToken } from "./tokenResponse.ts";

export type LoginRequest = components["schemas"]["LoginRequest"];
export type LoginResponse = components["schemas"]["LoginResponse"];
export type AuthCallOptions = Pick<RequestOptions, "signal">;

export async function loginApi(
    input: LoginRequest,
    options: AuthCallOptions = {},
): Promise<LoginResponse> {
    return request(
        async (client) => {
            const { data, response } = await client.POST("/api/auth/login", { body: input });
            return validateToken(data, response.status);
        },
        { ...options, cookieAction: "login" },
    );
}

export async function refreshApi(): Promise<LoginResponse> {
    // 공유 갱신에는 개별 화면의 AbortSignal을 연결하지 않는다.
    return request(
        async (client) => {
            const { data, response } = await client.POST("/api/auth/refresh");
            return validateToken(data, response.status);
        },
        { cookieAction: "refresh" },
    );
}

export async function logoutApi(): Promise<void> {
    await request(
        async (client) => {
            const { data, response } = await client.POST("/api/auth/logout");
            if (data !== undefined)
                throw new ApiError("invalid-response", { status: response.status });
        },
        { cookieAction: "logout" },
    );
}
