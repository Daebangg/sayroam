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
        | "completeSocial"
        | "cancelSocial"
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
        completeSocial: session.completeSocial,
        cancelSocial: session.cancelSocial,
    };
}
