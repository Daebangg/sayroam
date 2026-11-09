import { useEffect, useState, useSyncExternalStore } from "react";
import { createAuthController, type AuthController } from "./authController";
import type { AuthState } from "../../auth.types";

type AuthSession = AuthState & Pick<AuthController, "login" | "logout" | "restore" | "check">;

export function useAuthSession(): AuthSession {
    const [session] = useState(createAuthController);
    const state = useSyncExternalStore(session.subscribe, session.getSnapshot);
    useEffect(() => {
        const disconnect = session.connect();
        void session.restore();
        const check = (): void => {
            void session.check();
        };
        window.addEventListener("focus", check);
        return () => {
            window.removeEventListener("focus", check);
            disconnect();
        };
    }, [session]);
    return {
        ...state,
        login: session.login,
        logout: session.logout,
        restore: session.restore,
        check: session.check,
    };
}
