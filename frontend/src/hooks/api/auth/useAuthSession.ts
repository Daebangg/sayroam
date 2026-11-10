import { useEffect, useState, useSyncExternalStore } from "react";
import { createAuthController, type AuthController } from "./authController";
import type { AuthState } from "../../auth.types";

type AuthSession = AuthState &
    Pick<
        AuthController,
        | "login"
        | "logout"
        | "restore"
        | "check"
        | "exchangeSocial"
        | "cancelSocial"
        | "resetDialog"
        | "updateNickname"
        | "endSession"
    >;

export function useAuthSession(socialReturn = false): AuthSession {
    const [session] = useState(createAuthController);
    const state = useSyncExternalStore(session.subscribe, session.getSnapshot);
    useEffect(() => {
        const disconnect = session.connect();
        if (!socialReturn) void session.restore();
        const check = (): void => {
            void session.check();
        };
        window.addEventListener("focus", check);
        return () => {
            window.removeEventListener("focus", check);
            disconnect();
        };
    }, [session, socialReturn]);
    return {
        ...state,
        login: session.login,
        logout: session.logout,
        restore: session.restore,
        check: session.check,
        exchangeSocial: session.exchangeSocial,
        cancelSocial: session.cancelSocial,
        resetDialog: session.resetDialog,
        updateNickname: session.updateNickname,
        endSession: session.endSession,
    };
}
