import { useEffect, useEffectEvent, useRef, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import type { PickedLocation, TripPlace } from "@/hooks/trip.types";
import type { TripSection } from "./tripDays";

// 개발·소량 트래픽용 OSM 공식 타일이다. 운영 배포 전에 키를 쓰는 타일 제공자로 바꾼다.
const TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const TILE_ATTRIBUTION =
    '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
// 장소가 없을 때 대한민국 전체가 보이도록 맞춘다.
const KOREA_BOUNDS = L.latLngBounds([33.0, 124.6], [38.7, 131.0]);

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

function pinIcon(section: TripSection, order: number): L.DivIcon {
    const tone = section.tone === null ? "trip-pin--unscheduled" : `trip-pin--tone-${section.tone}`;
    return L.divIcon({
        className: "",
        html: `<span class="trip-pin ${tone}"><span>${section.day === null ? "·" : order}</span></span>`,
        iconSize: [30, 30],
        iconAnchor: [4, 30],
        popupAnchor: [11, -28],
    });
}

// 장소 이름은 회원 입력일 수 있으므로 HTML 문자열로 합치지 않고 텍스트 노드로 넣는다.
function popupContent(place: TripPlace, label: string): HTMLElement {
    const root = document.createElement("div");
    root.className = "trip-map-popup";
    const meta = document.createElement("span");
    meta.textContent = label;
    const name = document.createElement("strong");
    name.textContent = place.name;
    root.append(meta, name);
    const detail = place.memo ?? place.address;
    if (detail) {
        const note = document.createElement("span");
        note.textContent = detail;
        root.append(note);
    }
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
    const { t } = useTranslation();
    const host = useRef<HTMLDivElement>(null);
    const map = useRef<L.Map | null>(null);
    const layer = useRef<L.LayerGroup | null>(null);
    const markers = useRef(new Map<number, L.Marker>());
    const pickedMarker = useRef<L.CircleMarker | null>(null);
    const selectPlace = useEffectEvent(onSelectPlace);
    const pick = useEffectEvent(onPick);
    const pickingNow = useRef(picking);

    useEffect(() => {
        pickingNow.current = picking;
    }, [picking]);

    useEffect(() => {
        const element = host.current;
        if (!element) return;
        const instance = L.map(element, { zoomControl: false });
        L.control.zoom({ position: "bottomright" }).addTo(instance);
        L.tileLayer(TILE_URL, { maxZoom: 19, attribution: TILE_ATTRIBUTION }).addTo(instance);
        instance.fitBounds(KOREA_BOUNDS);
        instance.on("click", (event: L.LeafletMouseEvent) => {
            if (pickingNow.current)
                pick({ latitude: event.latlng.lat, longitude: event.latlng.lng });
        });
        layer.current = L.layerGroup().addTo(instance);
        map.current = instance;
        const observer = new ResizeObserver(() => instance.invalidateSize());
        observer.observe(element);
        return () => {
            observer.disconnect();
            instance.remove();
            map.current = null;
            layer.current = null;
        };
    }, []);

    useEffect(() => {
        const instance = map.current;
        const group = layer.current;
        if (!instance || !group) return;
        group.clearLayers();
        markers.current.clear();
        for (const section of sections) {
            if (!visible.has(section.key)) continue;
            const label =
                section.day === null
                    ? t("tripDetail.unscheduled")
                    : t("tripDetail.dayTitle", { day: section.day });
            section.places.forEach((place, index) => {
                const marker = L.marker([place.latitude, place.longitude], {
                    icon: pinIcon(section, index + 1),
                    title: place.name,
                    riseOnHover: true,
                })
                    .bindPopup(popupContent(place, label))
                    .on("click", () => selectPlace(place.tripPlaceId))
                    .addTo(group);
                markers.current.set(place.tripPlaceId, marker);
            });
        }
        const points = [...markers.current.values()].map((marker) => marker.getLatLng());
        if (points.length > 0)
            instance.fitBounds(L.latLngBounds(points), { padding: [60, 60], maxZoom: 15 });
    }, [sections, visible, t]);

    useEffect(() => {
        const instance = map.current;
        const marker = focus ? markers.current.get(focus.placeId) : undefined;
        if (!instance || !marker) return;
        instance.setView(marker.getLatLng(), Math.max(instance.getZoom(), 15), { animate: true });
        marker.openPopup();
    }, [focus]);

    useEffect(() => {
        const instance = map.current;
        if (!instance) return;
        pickedMarker.current?.remove();
        pickedMarker.current = picked
            ? L.circleMarker([picked.latitude, picked.longitude], {
                  radius: 10,
                  className: "trip-map-picked",
              }).addTo(instance)
            : null;
    }, [picked]);

    return (
        <div className={`trip-map${picking ? " trip-map--picking" : ""}`}>
            {children}
            <div
                ref={host}
                className="trip-map__canvas"
                role="region"
                aria-label={t("tripDetail.map")}
            />
        </div>
    );
}
