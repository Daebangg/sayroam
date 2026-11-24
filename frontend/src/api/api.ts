import {
    validateAuthorization,
    validateProviders,
    validateSocialExchange,
} from "./auth/socialResponse.ts";
import { refreshAccessToken } from "./auth/refreshSession.ts";
import { validateToken } from "./auth/tokenResponse.ts";
import type { AccessTokenSnapshot } from "./auth/tokenStore.ts";
import { apiRoutes } from "./generated/schema.ts";
import { createApiRouter } from "./router.ts";
import {
    validateNotificationList,
    validateNotificationSummary,
} from "./notifications/notificationResponse.ts";
import {
    validateAddedPlace,
    validateCreatedRoom,
    validateCreatedTrip,
    validateInvitation,
    validateJoinedRoom,
    validatePlaceSearch,
    validateTrip,
    validateTripList,
    validateTripPlaces,
    validateUpdatedTrip,
} from "./trips/tripResponse.ts";
import { validateCurrentUser, validateLoginMethods } from "./users/userResponse.ts";
import {
    validateIssued,
    validateResetProof,
    validateSignup,
    validateSignupProof,
    validateAccountIssued,
    validateAccountProof,
} from "./auth/emailResponse.ts";

function recoverSession(revision: number): Promise<AccessTokenSnapshot> {
    return refreshAccessToken(() => api.call("refresh"), revision);
}

export const api = createApiRouter(apiRoutes, {
    recover: recoverSession,
    extensions: {
        listMyNotifications: { parseResponse: validateNotificationList },
        getMyNotificationSummary: { parseResponse: validateNotificationSummary },
        listMyTrips: { parseResponse: validateTripList },
        createTrip: { parseResponse: validateCreatedTrip },
        getTrip: { parseResponse: validateTrip },
        updateTrip: { parseResponse: validateUpdatedTrip },
        listTripPlaces: { parseResponse: validateTripPlaces },
        addTripPlace: { parseResponse: validateAddedPlace },
        addTripCustomPlace: { parseResponse: validateAddedPlace },
        searchPlaces: { parseResponse: validatePlaceSearch },
        createTripRoom: { parseResponse: validateCreatedRoom },
        createInvitation: { parseResponse: validateInvitation },
        joinByInvitation: { parseResponse: validateJoinedRoom },
        listSocialProviders: { parseResponse: validateProviders },
        startSocialLogin: { cookieAction: "oauth", parseResponse: validateAuthorization },
        exchangeSocialLoginResult: {
            cookieAction: "social",
            parseResponse: validateSocialExchange,
        },
        completeSocialSignup: { cookieAction: "login", parseResponse: validateToken },
        login: { cookieAction: "login", parseResponse: validateToken },
        refresh: { cookieAction: "refresh", parseResponse: validateToken },
        logout: { cookieAction: "logout" },
        me: { parseResponse: validateCurrentUser },
        loginMethods: { parseResponse: validateLoginMethods },
        updateProfile: { parseResponse: validateCurrentUser },
        uploadProfileImage: { parseResponse: validateCurrentUser },
        request: { parseResponse: validateIssued },
        requestPasswordReset: { parseResponse: validateIssued },
        confirm: { parseResponse: validateSignupProof },
        confirmPasswordReset: { parseResponse: validateResetProof },
        signup: { parseResponse: validateSignup },
        resetPassword: { cookieAction: "logout" },
        request_1: { parseResponse: validateAccountIssued },
        confirm_1: { parseResponse: validateAccountProof },
        change: { cookieAction: "account" },
        delete: { cookieAction: "account" },
    },
});
