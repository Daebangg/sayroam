import { useRef, useState } from "react";
import { api } from "@/api/api";

type CreateTrip = {
    busy: boolean;
    failed: boolean;
    /** 생성한 여행 ID. 진행 중인 요청이 있거나 실패하면 null */
    create: (
        destinationId: number,
        startDate: string,
        endDate: string,
        title: string,
    ) => Promise<number | null>;
};

/** 여행 생성은 반복하면 여행이 여러 개 생기므로 진행 중에는 다시 보내지 않고 자동 재시도도 하지 않는다. */
export function useCreateTrip(): CreateTrip {
    const [busy, setBusy] = useState(false);
    const [failed, setFailed] = useState(false);
    const pending = useRef(false);

    async function create(
        destinationId: number,
        startDate: string,
        endDate: string,
        title: string,
    ): Promise<number | null> {
        if (pending.current) return null;
        pending.current = true;
        setBusy(true);
        setFailed(false);
        try {
            const response = await api.call("createTrip", {
                body: { destinationId, startDate, endDate, title },
            });
            return response.tripId;
        } catch {
            setFailed(true);
            return null;
        } finally {
            pending.current = false;
            setBusy(false);
        }
    }

    return { busy, failed, create };
}
