import { useEffect, useEffectEvent } from "react";
import { subscribeBadGateway } from "../api/serverErrors";

export function useServerErrors(onBadGateway: () => void): void {
    const notify = useEffectEvent(onBadGateway);
    useEffect(() => subscribeBadGateway(() => notify()), []);
}
