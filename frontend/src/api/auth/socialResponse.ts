import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";
import { validateToken } from "./tokenResponse.ts";

const authorizationEndpoints = {
    GOOGLE: "https://accounts.google.com/o/oauth2/v2/auth",
    KAKAO: "https://kauth.kakao.com/oauth/authorize",
    NAVER: "https://nid.naver.com/oauth2.0/authorize",
} as const;
export type SocialProvider = components["schemas"]["SocialProvidersResponse"]["providers"][number];
export function isSocialProvider(value: unknown): value is SocialProvider {
    return typeof value === "string" && Object.hasOwn(authorizationEndpoints, value);
}
function record(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}
function seconds(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}
export function validateProviders(
    value: unknown,
    status: number,
): components["schemas"]["SocialProvidersResponse"] {
    if (
        !record(value) ||
        !Array.isArray(value.providers) ||
        !value.providers.every(isSocialProvider)
    )
        throw new ApiError("invalid-response", { status });
    return { providers: [...new Set(value.providers)] };
}
export function validateAuthorization(
    value: unknown,
    status: number,
): components["schemas"]["SocialAuthStartResponse"] {
    if (
        !record(value) ||
        typeof value.authorizationUrl !== "string" ||
        !seconds(value.expiresInSeconds)
    )
        throw new ApiError("invalid-response", { status });
    const url = new URL(value.authorizationUrl);
    if (
        !Object.values(authorizationEndpoints).some(
            (endpoint) => url.origin + url.pathname === endpoint,
        ) ||
        url.username ||
        url.password ||
        url.hash
    )
        throw new ApiError("invalid-response", { status });
    return { authorizationUrl: url.href, expiresInSeconds: value.expiresInSeconds };
}
export function authorizationMatches(provider: SocialProvider, address: string): boolean {
    const url = new URL(address);
    return url.origin + url.pathname === authorizationEndpoints[provider];
}
export function validateSocialExchange(
    value: unknown,
    status: number,
): components["schemas"]["SocialExchangeResponse"] {
    if (!record(value)) throw new ApiError("invalid-response", { status });
    if (value.status === "AUTHENTICATED" && value.signup == null)
        return { status: "AUTHENTICATED", login: validateToken(value.login, status), signup: null };
    const signup = value.signup;
    if (
        value.status !== "SIGNUP_REQUIRED" ||
        value.login != null ||
        !record(signup) ||
        typeof signup.signupTicket !== "string" ||
        !/^[A-Za-z0-9_-]{43}$/.test(signup.signupTicket) ||
        !seconds(signup.expiresInSeconds) ||
        typeof signup.emailVerificationRequired !== "boolean" ||
        !(
            signup.suggestedEmail == null ||
            (typeof signup.suggestedEmail === "string" && signup.suggestedEmail.length <= 254)
        ) ||
        (!signup.emailVerificationRequired && !signup.suggestedEmail)
    )
        throw new ApiError("invalid-response", { status });
    return {
        status: "SIGNUP_REQUIRED",
        login: null,
        signup: {
            signupTicket: signup.signupTicket,
            expiresInSeconds: signup.expiresInSeconds,
            emailVerificationRequired: signup.emailVerificationRequired,
            suggestedEmail: signup.suggestedEmail ?? null,
        },
    };
}
