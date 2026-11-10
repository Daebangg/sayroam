import { useEffect, useRef, useState } from "react";
import { api } from "../../../api/api.ts";
import { ApiError } from "../../../api/apiError.ts";
import { authorizationMatches, type SocialProvider } from "../../../api/auth/socialResponse.ts";
import { rememberSocialReturn, forgetSocialReturn } from "../../../routing/socialReturn.ts";
import type { AuthMessage } from "../../auth.types.ts";
import { socialMessageFor } from "./authController.ts";

export function useSocialLogin(active = true) {
    const [providers, setProviders] = useState<SocialProvider[]>([]);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState<AuthMessage | null>(null);
    const pending = useRef(false);
    const startRequest = useRef<AbortController | null>(null);
    useEffect(() => {
        const onPageShow = (event: PageTransitionEvent): void => {
            if (!event.persisted || !pending.current) return;
            pending.current = false;
            setBusy(false);
            setMessage("socialCancelled");
            try {
                forgetSocialReturn();
            } catch {
                /* 새 인증으로 복구한다. */
            }
        };
        window.addEventListener("pageshow", onPageShow);
        return () => {
            startRequest.current?.abort();
            window.removeEventListener("pageshow", onPageShow);
        };
    }, []);
    useEffect(() => {
        if (!active) return;
        const controller = new AbortController();
        void api
            .call("listSocialProviders", { signal: controller.signal, cache: "no-store" })
            .then((result) => {
                if (!controller.signal.aborted) {
                    setProviders(result.providers);
                    setMessage(null);
                }
            })
            .catch(() => {
                if (!controller.signal.aborted) setMessage("socialUnavailable");
            });
        return () => controller.abort();
    }, [active]);
    async function start(provider: SocialProvider): Promise<void> {
        if (pending.current) return;
        if (!providers.includes(provider)) {
            setMessage("socialDisabled");
            return;
        }
        const controller = new AbortController();
        startRequest.current = controller;
        pending.current = true;
        setBusy(true);
        setMessage(null);
        try {
            rememberSocialReturn();
            const response = await api.call("startSocialLogin", {
                params: { path: { provider: provider.toLowerCase() } },
                body: { returnPath: "/" },
                signal: controller.signal,
            });
            if (!authorizationMatches(provider, response.authorizationUrl))
                throw new ApiError("invalid-response");
            window.location.assign(response.authorizationUrl);
        } catch (error) {
            if (controller.signal.aborted) return;
            try {
                forgetSocialReturn();
            } catch {
                /* 저장소 장애도 새 인증으로 복구한다. */
            }
            if (error instanceof ApiError && error.problem?.code === "SOCIAL_PROVIDER_DISABLED")
                setProviders((values) => values.filter((value) => value !== provider));
            setMessage(socialMessageFor(error));
            pending.current = false;
            setBusy(false);
        }
    }
    return { providers, busy, message, start, clearMessage: () => setMessage(null) };
}
