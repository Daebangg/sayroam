import { useEffect, useEffectEvent, useRef, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { useTheme } from "@/hooks/useTheme";
import type { PickedLocation, TripPlace } from "@/hooks/trip.types";
import {
    loadGoogleMaps,
    mapId,
    onGoogleMapsAuthFailure,
    type GoogleMapsLibraries,
} from "./googleMapsLoader";
import type { TripSection } from "./tripDays";

// 장소가 없을 때 대한민국 전체가 보이도록 맞춘다.
const KOREA_BOUNDS: google.maps.LatLngBoundsLiteral = {
    south: 33.0,
    west: 124.6,
    north: 38.7,
    east: 131.0,
};
const FIT_PADDING = 60;
// 장소가 하나뿐이어도 지나치게 확대되지 않도록 맞춤 후 확대를 이 값으로 제한한다.
const MAX_FIT_ZOOM = 15;
const FOCUS_ZOOM = 15;

type MapStatus = "loading" | "ready" | "unavailable";

type PlaceMarker = {
    marker: google.maps.marker.AdvancedMarkerElement;
    label: string;
    content: HTMLElement;
};

type TripMapProps = {
    sections: TripSection[];
    visible: ReadonlySet<string>;
    /** 목록에서 고른 장소. seq가 바뀔 때마다 그 장소로 이동한다. */
    focus: { placeId: number; seq: number } | null;
    onSelectPlace: (placeId: number) => void;
    picking: boolean;
    picked: PickedLocation | null;
    onPick: (location: PickedLocation) => void;
    /** 지도 위에 띄우는 조작(일정 선택 등) */
    children?: ReactNode;
};

function pinElement(section: TripSection, order: number): HTMLElement {
    const tone = section.tone === null ? "trip-pin--unscheduled" : `trip-pin--tone-${section.tone}`;
    const root = document.createElement("span");
    root.className = "trip-map-marker";
    const pin = document.createElement("span");
    pin.className = `trip-pin ${tone}`;
    const label = document.createElement("span");
    label.textContent = section.day === null ? "·" : String(order);
    pin.append(label);
    root.append(pin);
    return root;
}

// 장소 이름은 회원 입력일 수 있으므로 HTML 문자열로 합치지 않고 텍스트 노드로 넣는다.
function popupContent(place: TripPlace): HTMLElement {
    const root = document.createElement("div");
    root.className = "trip-map-popup";
    const name = document.createElement("strong");
    name.textContent = place.name;
    root.append(name);
    const detail = place.memo ?? place.address;
    if (detail) {
        const note = document.createElement("span");
        note.textContent = detail;
        root.append(note);
    }
    return root;
}

// 팝업 머리글(닫기 버튼 줄)이 비지 않도록 일차 이름을 머리글에 둔다.
function openPopup(
    popup: google.maps.InfoWindow,
    instance: google.maps.Map,
    target: PlaceMarker,
): void {
    const header = document.createElement("span");
    header.className = "trip-map-popup__label";
    header.textContent = target.label;
    popup.setHeaderContent(header);
    popup.setContent(target.content);
    popup.open({ anchor: target.marker, map: instance });
}

function pickedElement(): HTMLElement {
    const root = document.createElement("span");
    root.className = "trip-map-picked";
    return root;
}

export function TripMap({
    sections,
    visible,
    focus,
    onSelectPlace,
    picking,
    picked,
    onPick,
    children,
}: TripMapProps) {
    const { t, i18n } = useTranslation();
    const { resolved } = useTheme();
    const [status, setStatus] = useState<MapStatus>("loading");
    const host = useRef<HTMLDivElement>(null);
    const libraries = useRef<GoogleMapsLibraries | null>(null);
    const map = useRef<google.maps.Map | null>(null);
    const infoWindow = useRef<google.maps.InfoWindow | null>(null);
    const markers = useRef(new Map<number, PlaceMarker>());
    const pickedMarker = useRef<google.maps.marker.AdvancedMarkerElement | null>(null);
    // 지도는 화면에 들어올 때 한 번만 만든다. 다시 만들 때마다 지도 로드가 과금되므로
    // 언어·테마는 만들 때의 값을 쓰고, 이후 바뀌어도 지도를 다시 만들지 않는다.
    const initial = useRef({ language: i18n.resolvedLanguage ?? "ko", theme: resolved });
    const selectPlace = useEffectEvent(onSelectPlace);
    const pick = useEffectEvent(onPick);
    const pickingNow = useRef(picking);

    useEffect(() => {
        pickingNow.current = picking;
        map.current?.setOptions({ draggableCursor: picking ? "crosshair" : null });
    }, [picking, status]);

    useEffect(() => {
        const element = host.current;
        if (!element) return;
        const placeMarkers = markers.current;
        let cancelled = false;
        let clickListener: google.maps.MapsEventListener | null = null;
        const stopAuthFailure = onGoogleMapsAuthFailure(() => setStatus("unavailable"));
        loadGoogleMaps(initial.current.language)
            .then((loaded) => {
                if (cancelled) return;
                const instance = new loaded.maps.Map(element, {
                    mapId: mapId ?? null,
                    colorScheme: initial.current.theme === "dark" ? "DARK" : "LIGHT",
                    disableDefaultUI: true,
                    zoomControl: true,
                    zoomControlOptions: { position: google.maps.ControlPosition.RIGHT_BOTTOM },
                    // 지도에 기본으로 보이는 장소는 아직 일정에 추가할 수 없으므로 눌러도 반응하지 않는다.
                    clickableIcons: false,
                    gestureHandling: "greedy",
                });
                instance.fitBounds(KOREA_BOUNDS);
                clickListener = instance.addListener(
                    "click",
                    (event: google.maps.MapMouseEvent) => {
                        if (pickingNow.current && event.latLng)
                            pick({ latitude: event.latLng.lat(), longitude: event.latLng.lng() });
                    },
                );
                libraries.current = loaded;
                map.current = instance;
                infoWindow.current = new loaded.maps.InfoWindow();
                setStatus("ready");
            })
            .catch(() => {
                if (!cancelled) setStatus("unavailable");
            });
        return () => {
            cancelled = true;
            stopAuthFailure();
            clickListener?.remove();
            infoWindow.current?.close();
            for (const { marker } of placeMarkers.values()) marker.map = null;
            placeMarkers.clear();
            if (pickedMarker.current) pickedMarker.current.map = null;
            pickedMarker.current = null;
            infoWindow.current = null;
            libraries.current = null;
            map.current = null;
        };
    }, []);

    useEffect(() => {
        const instance = map.current;
        const loaded = libraries.current;
        const popup = infoWindow.current;
        if (status !== "ready" || !instance || !loaded || !popup) return;
        popup.close();
        for (const { marker } of markers.current.values()) marker.map = null;
        markers.current.clear();
        const bounds = new google.maps.LatLngBounds();
        for (const section of sections) {
            if (!visible.has(section.key)) continue;
            const label =
                section.day === null
                    ? t("tripDetail.unscheduled")
                    : t("tripDetail.dayTitle", { day: section.day });
            section.places.forEach((place, index) => {
                const position = { lat: place.latitude, lng: place.longitude };
                const marker = new loaded.marker.AdvancedMarkerElement({
                    map: instance,
                    position,
                    content: pinElement(section, index + 1),
                    title: place.name,
                    gmpClickable: true,
                });
                const target = { marker, label, content: popupContent(place) };
                marker.addListener("click", () => {
                    openPopup(popup, instance, target);
                    selectPlace(place.tripPlaceId);
                });
                markers.current.set(place.tripPlaceId, target);
                bounds.extend(position);
            });
        }
        if (markers.current.size === 0) return;
        instance.fitBounds(bounds, FIT_PADDING);
        google.maps.event.addListenerOnce(instance, "idle", () => {
            if ((instance.getZoom() ?? 0) > MAX_FIT_ZOOM) instance.setZoom(MAX_FIT_ZOOM);
        });
    }, [status, sections, visible, t]);

    useEffect(() => {
        const instance = map.current;
        const popup = infoWindow.current;
        const target = focus ? markers.current.get(focus.placeId) : undefined;
        if (!instance || !popup || !target?.marker.position) return;
        instance.panTo(target.marker.position);
        instance.setZoom(Math.max(instance.getZoom() ?? 0, FOCUS_ZOOM));
        openPopup(popup, instance, target);
    }, [focus]);

    useEffect(() => {
        const instance = map.current;
        const loaded = libraries.current;
        if (pickedMarker.current) pickedMarker.current.map = null;
        pickedMarker.current =
            status === "ready" && instance && loaded && picked
                ? new loaded.marker.AdvancedMarkerElement({
                      map: instance,
                      position: { lat: picked.latitude, lng: picked.longitude },
                      content: pickedElement(),
                  })
                : null;
    }, [status, picked]);

    return (
        <div className="trip-map">
            {children}
            <div
                ref={host}
                className="trip-map__canvas"
                role="region"
                aria-label={t("tripDetail.map")}
            />
            {status === "unavailable" && (
                <p className="trip-map__notice" role="status">
                    {t("tripDetail.mapUnavailable")}
                </p>
            )}
        </div>
    );
}
