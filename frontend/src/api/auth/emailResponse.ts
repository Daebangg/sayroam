import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

function record(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}
function seconds(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}
function proof(value: unknown): value is string {
    return typeof value === "string" && /^[A-Za-z0-9_-]{43}$/.test(value);
}
export function validateIssued(
    value: unknown,
    status: number,
): components["schemas"]["IssuedRequest"] {
    if (
        !record(value) ||
        typeof value.verificationId !== "string" ||
        !/^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(value.verificationId) ||
        !seconds(value.expiresInSeconds) ||
        typeof value.resendAfterSeconds !== "number" ||
        !Number.isSafeInteger(value.resendAfterSeconds) ||
        value.resendAfterSeconds < 0
    )
        throw new ApiError("invalid-response", { status });
    return {
        verificationId: value.verificationId,
        expiresInSeconds: value.expiresInSeconds,
        resendAfterSeconds: value.resendAfterSeconds,
    };
}
export function validateSignupProof(
    value: unknown,
    status: number,
): components["schemas"]["EmailVerificationConfirmResponse"] {
    if (!record(value) || !proof(value.signupProof) || !seconds(value.expiresInSeconds))
        throw new ApiError("invalid-response", { status });
    return { signupProof: value.signupProof, expiresInSeconds: value.expiresInSeconds };
}
export function validateResetProof(
    value: unknown,
    status: number,
): components["schemas"]["PasswordResetConfirmResponse"] {
    if (!record(value) || !proof(value.resetProof) || !seconds(value.expiresInSeconds))
        throw new ApiError("invalid-response", { status });
    return { resetProof: value.resetProof, expiresInSeconds: value.expiresInSeconds };
}
export function validateSignup(
    value: unknown,
    status: number,
): components["schemas"]["SignupResponse"] {
    if (!record(value) || !seconds(value.userId))
        throw new ApiError("invalid-response", { status });
    return { userId: value.userId };
}

export function validateAccountIssued(
    value: unknown,
    status: number,
): components["schemas"]["AccountVerificationResponse"] {
    const issued = validateIssued(value, status);
    if (
        !record(value) ||
        (value.purpose !== "PASSWORD_CHANGE" && value.purpose !== "ACCOUNT_DELETION")
    )
        throw new ApiError("invalid-response", { status });
    return { ...issued, purpose: value.purpose };
}

export function validateAccountProof(
    value: unknown,
    status: number,
): components["schemas"]["AccountVerificationConfirmResponse"] {
    if (
        !record(value) ||
        !proof(value.accountProof) ||
        !seconds(value.expiresInSeconds) ||
        (value.purpose !== "PASSWORD_CHANGE" && value.purpose !== "ACCOUNT_DELETION")
    )
        throw new ApiError("invalid-response", { status });
    return {
        accountProof: value.accountProof,
        expiresInSeconds: value.expiresInSeconds,
        purpose: value.purpose,
    };
}
