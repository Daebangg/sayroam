import { useEffect, useState } from "react";
import { api } from "@/api/api";
import type { MyTrip } from "../../trip.types";

export type MyTripsStatus = "loading" | "ready" | "failed";

type MyTrips = { trips: MyTrip[]; status: MyTripsStatus; reload: () => void };

/** 회원이 바뀌면 호출하는 쪽에서 key로 다시 마운트해 이전 회원의 목록을 남기지 않는다. */
export function useMyTrips(userId: number): MyTrips {
    const [trips, setTrips] = useState<MyTrip[]>([]);
    const [status, setStatus] = useState<MyTripsStatus>("loading");
    const [revision, setRevision] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        void api
            .call("listMyTrips", { signal: controller.signal })
            .then((response) => {
                if (controller.signal.aborted) return;
                setTrips(response.trips);
                setStatus("ready");
            })
            .catch(() => {
                if (!controller.signal.aborted) setStatus("failed");
            });
        return () => controller.abort();
    }, [userId, revision]);

    function reload(): void {
        setStatus("loading");
        setRevision((value) => value + 1);
    }

    return { trips, status, reload };
}
