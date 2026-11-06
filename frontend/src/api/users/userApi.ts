import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";
import { request, type RequestOptions } from "../http.ts";

export type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];
export type UserCallOptions = Pick<RequestOptions, "signal">;

export async function getCurrentUser(options: UserCallOptions = {}): Promise<CurrentUserResponse> {
    return request(
        async (client) => {
            const { data, response } = await client.GET("/api/users/me");
            return validateCurrentUser(data, response.status);
        },
        { ...options, auth: "required" },
    );
}

function validateCurrentUser(value: unknown, status: number): CurrentUserResponse {
    if (
        typeof value !== "object" ||
        value === null ||
        !("userId" in value) ||
        typeof value.userId !== "number" ||
        !Number.isSafeInteger(value.userId) ||
        value.userId <= 0 ||
        !("email" in value) ||
        (value.email !== null && typeof value.email !== "string") ||
        !("nickname" in value) ||
        typeof value.nickname !== "string" ||
        !("profileImageUrl" in value) ||
        (value.profileImageUrl !== null && typeof value.profileImageUrl !== "string") ||
        !("role" in value) ||
        (value.role !== "USER" && value.role !== "ADMIN")
    )
        throw new ApiError("invalid-response", { status });
    return {
        userId: value.userId,
        email: value.email,
        nickname: value.nickname,
        profileImageUrl: value.profileImageUrl,
        role: value.role,
    };
}
