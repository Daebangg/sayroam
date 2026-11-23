import { useEffect, useState } from "react";
import { api } from "@/api/api";
import type { SearchedPlace } from "../../trip.types";

export type PlaceSearchStatus = "idle" | "loading" | "ready" | "failed";

type Result = { query: string; places: SearchedPlace[]; failed: boolean };

const DEBOUNCE_MS = 300;
export const PLACE_QUERY_MAX_LENGTH = 100;

/** 입력이 멈춘 뒤 검색하고, 새 입력이 오면 이전 요청을 취소한다. */
export function usePlaceSearch(query: string): {
    places: SearchedPlace[];
    status: PlaceSearchStatus;
} {
    const keyword = query.trim();
    const [result, setResult] = useState<Result | null>(null);

    useEffect(() => {
        if (!keyword) return;
        const controller = new AbortController();
        const timer = window.setTimeout(() => {
            void api
                .call("searchPlaces", {
                    params: { query: { query: keyword } },
                    signal: controller.signal,
                })
                .then((response) => {
                    if (!controller.signal.aborted)
                        setResult({ query: keyword, places: response.places, failed: false });
                })
                .catch(() => {
                    if (!controller.signal.aborted)
                        setResult({ query: keyword, places: [], failed: true });
                });
        }, DEBOUNCE_MS);
        return () => {
            window.clearTimeout(timer);
            controller.abort();
        };
    }, [keyword]);

    if (!keyword) return { places: [], status: "idle" };
    if (result?.query !== keyword) return { places: [], status: "loading" };
    return { places: result.places, status: result.failed ? "failed" : "ready" };
}
