import { refreshAccessToken } from "./auth/refreshSession.ts";
import { validateToken } from "./auth/tokenResponse.ts";
import type { AccessTokenSnapshot } from "./auth/tokenStore.ts";
import { apiRoutes } from "./generated/schema.ts";
import { createApiRouter } from "./router.ts";
import { validateCurrentUser } from "./users/userResponse.ts";

function recoverSession(revision: number): Promise<AccessTokenSnapshot> {
    return refreshAccessToken(() => api.call("refresh"), revision);
}

export const api = createApiRouter(apiRoutes, {
    recover: recoverSession,
    extensions: {
        login: { cookieAction: "login", parseResponse: validateToken },
        refresh: { cookieAction: "refresh", parseResponse: validateToken },
        logout: { cookieAction: "logout" },
        me: { parseResponse: validateCurrentUser },
        updateProfile: { parseResponse: validateCurrentUser },
    },
});
