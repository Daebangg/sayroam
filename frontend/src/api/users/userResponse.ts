import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];

function isBirthDate(value: unknown): value is string | null {
    if (value === null) return true;
    if (
        typeof value !== "string" ||
        !/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(value) ||
        value.startsWith("0000")
    )
        return false;
    const parsed = new Date(value);
    return Number.isFinite(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

export function validateCurrentUser(value: unknown, status: number): CurrentUserResponse {
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
        !("gender" in value) ||
        (value.gender !== null && value.gender !== "MALE" && value.gender !== "FEMALE") ||
        !("birthYear" in value) ||
        (value.birthYear !== null &&
            (typeof value.birthYear !== "number" ||
                !Number.isSafeInteger(value.birthYear) ||
                value.birthYear <= 0)) ||
        !("birthDate" in value) ||
        !isBirthDate(value.birthDate) ||
        !("createdAt" in value) ||
        typeof value.createdAt !== "string" ||
        !value.createdAt.endsWith("Z") ||
        !Number.isFinite(Date.parse(value.createdAt)) ||
        !("role" in value) ||
        (value.role !== "USER" && value.role !== "ADMIN")
    )
        throw new ApiError("invalid-response", { status });
    return {
        userId: value.userId,
        email: value.email,
        nickname: value.nickname,
        profileImageUrl: value.profileImageUrl,
        gender: value.gender,
        birthYear: value.birthYear,
        role: value.role,
        createdAt: value.createdAt,
        birthDate: value.birthDate,
    };
}

export function validateLoginMethods(
    value: unknown,
    status: number,
): components["schemas"]["UserLoginMethodsResponse"] {
    if (
        typeof value !== "object" ||
        value === null ||
        !("passwordEnabled" in value) ||
        typeof value.passwordEnabled !== "boolean" ||
        !("socialProviders" in value) ||
        !Array.isArray(value.socialProviders) ||
        !value.socialProviders.every(
            (provider: unknown) =>
                provider === "GOOGLE" || provider === "KAKAO" || provider === "NAVER",
        ) ||
        new Set(value.socialProviders).size !== value.socialProviders.length
    )
        throw new ApiError("invalid-response", { status });
    return { passwordEnabled: value.passwordEnabled, socialProviders: value.socialProviders };
}
