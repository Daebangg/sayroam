import { api } from "../api.ts";
import type { ApiCallOptions, ApiResponse } from "../router.ts";

export type CurrentUserResponse = ApiResponse<"me">;
export type UserCallOptions = Pick<ApiCallOptions<"me">, "signal">;

export function getCurrentUser(options: UserCallOptions = {}): Promise<CurrentUserResponse> {
    return api.call("me", options);
}
