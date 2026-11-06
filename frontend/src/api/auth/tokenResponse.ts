import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

type LoginResponse = components["schemas"]["LoginResponse"];

export function validateToken(value: LoginResponse): LoginResponse {
    // 구조 검증은 생성 SDK가 담당하고 토큰 사용에 필요한 추가 조건만 확인한다.
    assertTokenUsage(value.accessToken, value.expiresIn);
    return value;
}

export function assertTokenUsage(accessToken: unknown, expiresIn: unknown): void {
    if (
        typeof accessToken !== "string" ||
        !accessToken ||
        /\s/.test(accessToken) ||
        typeof expiresIn !== "number" ||
        expiresIn <= 0
    ) {
        throw new ApiError("invalid-response");
    }
}
