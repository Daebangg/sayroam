import { useEffect, useEffectEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, Spinner, TextField } from "@/components/ui";
import type { PickedLocation, SearchedPlace } from "@/hooks/trip.types";

export type PlaceSearchState = {
    places: SearchedPlace[];
    status: "idle" | "loading" | "ready" | "failed";
};

type PlaceAddBoxProps = {
    query: string;
    queryMaxLength: number;
    nameMaxLength: number;
    onQueryChange: (query: string) => void;
    search: PlaceSearchState;
    adding: boolean;
    onAddPlace: (place: SearchedPlace) => void;
    picking: boolean;
    picked: PickedLocation | null;
    onStartPicking: () => void;
    onCancelPicking: () => void;
    onAddCustom: (name: string) => void;
    onClose: () => void;
};

export function PlaceAddBox({
    query,
    queryMaxLength,
    nameMaxLength,
    onQueryChange,
    search,
    adding,
    onAddPlace,
    picking,
    picked,
    onStartPicking,
    onCancelPicking,
    onAddCustom,
    onClose,
}: PlaceAddBoxProps) {
    const { t } = useTranslation();
    const [name, setName] = useState("");
    const root = useRef<HTMLDivElement>(null);
    const close = useEffectEvent(onClose);

    // 상자 바깥을 누르면 닫는다. 지도에서 위치를 고르는 중에는 지도를 눌러야 하므로 닫지 않는다.
    useEffect(() => {
        if (picking) return;
        function closeOutside(event: PointerEvent) {
            if (event.target instanceof Node && !root.current?.contains(event.target)) close();
        }
        document.addEventListener("pointerdown", closeOutside);
        return () => document.removeEventListener("pointerdown", closeOutside);
    }, [picking]);

    return (
        <div
            ref={root}
            className="place-add-box"
            onKeyDown={(event) => {
                if (event.key === "Escape") onClose();
            }}
        >
            <div className="place-add-box__search">
                <Icon name="search" variant="outline" />
                <TextField
                    label={t("tripDetail.searchLabel")}
                    placeholder={t("tripDetail.searchPlaceholder")}
                    value={query}
                    maxLength={queryMaxLength}
                    autoFocus
                    onChange={(event) => onQueryChange(event.currentTarget.value)}
                />
            </div>
            {search.status === "loading" && <Spinner label={t("tripDetail.searching")} />}
            {search.status === "failed" && (
                <p className="place-add-box__message" role="alert">
                    {t("tripDetail.searchFailed")}
                </p>
            )}
            {search.status === "ready" && search.places.length === 0 && (
                <p className="place-add-box__message" role="status">
                    {t("tripDetail.noResults")}
                </p>
            )}
            {search.places.length > 0 && (
                <ul className="place-add-box__results" aria-label={t("tripDetail.searchLabel")}>
                    {search.places.map((place) => (
                        <li key={place.sourceId}>
                            <Button
                                variant="plain"
                                className="place-add-box__result"
                                disabled={adding}
                                onClick={() => onAddPlace(place)}
                            >
                                <Icon name="pin" />
                                <span>
                                    <strong>{place.name}</strong>
                                    <span>
                                        {[t(`tripDetail.category.${place.category}`), place.address]
                                            .filter(Boolean)
                                            .join(" · ")}
                                    </span>
                                </span>
                            </Button>
                        </li>
                    ))}
                </ul>
            )}
            <div className="place-add-box__custom">
                {picking ? (
                    <>
                        <p className="place-add-box__message" role="status">
                            {picked ? t("tripDetail.picked") : t("tripDetail.pickHint")}
                        </p>
                        {picked && (
                            <form
                                className="place-add-box__custom-form"
                                onSubmit={(event) => {
                                    event.preventDefault();
                                    if (name.trim()) onAddCustom(name.trim());
                                }}
                            >
                                <TextField
                                    label={t("tripDetail.customName")}
                                    placeholder={t("tripDetail.customNamePlaceholder")}
                                    value={name}
                                    maxLength={nameMaxLength}
                                    required
                                    autoFocus
                                    onChange={(event) => setName(event.currentTarget.value)}
                                />
                                <Button type="submit" loading={adding} disabled={!name.trim()}>
                                    {t("tripDetail.addCustom")}
                                </Button>
                            </form>
                        )}
                        <Button variant="ghost" onClick={onCancelPicking}>
                            {t("tripDetail.pickCancel")}
                        </Button>
                    </>
                ) : (
                    <Button variant="ghost" onClick={onStartPicking}>
                        <Icon name="pin" variant="outline" />
                        {t("tripDetail.pickOnMap")}
                    </Button>
                )}
            </div>
        </div>
    );
}
