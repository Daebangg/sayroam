import { useEffect, useEffectEvent } from "react";
import { subscribeBadGateway } from "../api/serverErrors";

export function useServerErrors(onBadGateway: (path?: string) => void): void {
    const notify = useEffectEvent(onBadGateway);
    useEffect(() => subscribeBadGateway((path) => notify(path)), []);
}
