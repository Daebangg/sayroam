import { useEffect, useState } from "react";
import { api } from "@/api/api";
import type { components } from "@/api/generated/schema";

export type Destination = components["schemas"]["DestinationSummary"];
export type DestinationsStatus = "idle" | "loading" | "ready" | "failed";

type Destinations = { destinations: Destination[]; status: DestinationsStatus };

/** 국내 여행지 목록은 로그인한 회원만 받을 수 있어 userId가 없으면 요청하지 않는다. */
export function useDestinations(userId: number | undefined): Destinations {
    const [destinations, setDestinations] = useState<Destination[]>([]);
    const [status, setStatus] = useState<DestinationsStatus>("loading");

    useEffect(() => {
        if (userId === undefined) return;
        const controller = new AbortController();
        void api
            .call("listDestinations", { signal: controller.signal })
            .then((response) => {
                if (controller.signal.aborted) return;
                setDestinations(response.destinations);
                setStatus("ready");
            })
            .catch(() => {
                if (!controller.signal.aborted) setStatus("failed");
            });
        return () => controller.abort();
    }, [userId]);

    return userId === undefined ? { destinations: [], status: "idle" } : { destinations, status };
}
