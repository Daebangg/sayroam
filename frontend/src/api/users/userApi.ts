import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";
import type { RequestOptions } from "../http.ts";
import { me } from "../sdk.ts";

export type CurrentUserResponse = components["schemas"]["CurrentUserResponse"];
export type UserCallOptions = Pick<RequestOptions, "signal">;

export async function getCurrentUser(options: UserCallOptions = {}): Promise<CurrentUserResponse> {
    const value = await me(options);
    if (value.userId <= 0) throw new ApiError("invalid-response");
    return value;
}
