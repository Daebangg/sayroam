export type { SocialProvider } from "../api/auth/socialResponse.ts";
import type { LoginRequest } from "../api/auth/authApi.ts";
import type { CurrentUserResponse } from "../api/users/userApi.ts";

export type LoginInput = LoginRequest;
export type EmailFlowMessage =
    | "invalidNickname"
    | "invalidEmail"
    | "invalidPassword"
    | "codeInvalid"
    | "expired"
    | "limited"
    | "delivery"
    | "unavailable"
    | "conflict"
    | "registered"
    | "uncertain"
    | "security";
export type AuthMessage =
    | "limited"
    | "socialUnavailable"
    | "socialEmailRequired"
    | "socialCancelled"
    | "socialExpired"
    | "socialConflict"
    | "socialDisabled"
    | "socialUncertain"
    | "credentials"
    | "invalidInput"
    | "expired"
    | "unavailable"
    | "security"
    | "browserUnavailable"
    | "loginUncertain"
    | "logoutUncertain"
    | "profileUnavailable";

export type AuthState = Readonly<{
    status: "checking" | "anonymous" | "authenticated" | "signingIn" | "signingOut" | "unavailable";
    user: CurrentUserResponse | null;
    message: AuthMessage | null;
    failureStatus?: number;
    retryAt?: number;
    remainingAttempts?: number;
}>;
