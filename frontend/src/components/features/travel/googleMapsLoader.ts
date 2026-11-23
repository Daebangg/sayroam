import { importLibrary, setOptions } from "@googlemaps/js-api-loader";

export type GoogleMapsLibraries = {
    maps: google.maps.MapsLibrary;
    marker: google.maps.MarkerLibrary;
};

declare global {
    interface Window {
        /** 키가 잘못됐거나 허용되지 않은 주소에서 불렀을 때 Maps JavaScript API가 호출한다. */
        gm_authFailure?: () => void;
    }
}

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
export const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID;

let loading: Promise<GoogleMapsLibraries> | null = null;
let authFailed = false;
const authFailureListeners = new Set<() => void>();

/**
 * Maps JavaScript API를 한 번만 불러온다. 언어는 처음 부를 때의 값으로 고정된다.
 * 키나 지도 ID가 없으면 지도를 쓰지 않는 것으로 보고 실패한다.
 */
export function loadGoogleMaps(language: string): Promise<GoogleMapsLibraries> {
    if (!apiKey || !mapId) return Promise.reject(new Error("Google Maps is not configured"));
    if (authFailed) return Promise.reject(new Error("Google Maps authentication failed"));
    if (loading === null) {
        window.gm_authFailure = () => {
            authFailed = true;
            for (const listener of authFailureListeners) listener();
        };
        setOptions({ key: apiKey, v: "weekly", language, region: "KR" });
        loading = Promise.all([importLibrary("maps"), importLibrary("marker")]).then(
            ([maps, marker]) => ({ maps, marker }),
        );
    }
    return loading;
}

/** 지도를 만든 뒤에 알게 되는 인증 실패를 구독한다. */
export function onGoogleMapsAuthFailure(listener: () => void): () => void {
    authFailureListeners.add(listener);
    return () => {
        authFailureListeners.delete(listener);
    };
}
