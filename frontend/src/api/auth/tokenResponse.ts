import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

type LoginResponse = components["schemas"]["LoginResponse"];

export function validateToken(value: unknown, status: number): LoginResponse {
    if (
        typeof value !== "object" ||
        value === null ||
        !("accessToken" in value) ||
        typeof value.accessToken !== "string" ||
        !value.accessToken ||
        /\s/.test(value.accessToken) ||
        !("tokenType" in value) ||
        value.tokenType !== "Bearer" ||
        !("expiresIn" in value) ||
        typeof value.expiresIn !== "number" ||
        !Number.isSafeInteger(value.expiresIn) ||
        value.expiresIn <= 0
    ) {
        throw new ApiError("invalid-response", { status });
    }
    return {
        accessToken: value.accessToken,
        tokenType: value.tokenType,
        expiresIn: value.expiresIn,
    };
}
