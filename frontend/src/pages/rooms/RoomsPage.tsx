import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";
import { RoomPreviewRow } from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { roomPreviews, type PreviewRoom } from "@/data/preview";

export function RoomsPage() {
    const { t } = useTranslation();
    const [selected, setSelected] = useState<PreviewRoom | null>(null);
    const [creating, setCreating] = useState(false);
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className="workspace-page workspace-page--list travel-workspace"
        >
            <div className="page-heading">
                <div>
                    <p className="page-eyebrow">{t("mockup.stayConnected")}</p>
                    <h1>{t("rooms.heading")}</h1>
                </div>
                <Button variant="outline" onClick={() => setCreating(true)}>
                    <Icon name="plus" variant="outline" />
                    {t("rooms.create")}
                </Button>
            </div>
            <div className="travel-list-panel">
                {roomPreviews.map((room) => (
                    <RoomPreviewRow key={room.id} room={room} onOpen={() => setSelected(room)} />
                ))}
            </div>
            <Button
                variant="outline"
                className="room-create-mobile"
                onClick={() => setCreating(true)}
            >
                <Icon name="plus" variant="outline" />
                {t("rooms.create")}
            </Button>
            <p className="dashboard-footnote">{t("mockup.sampleData")}</p>
            <PreviewDialog
                open={selected !== null}
                title={selected ? t(selected.title) : ""}
                description={t("mockup.roomPreviewDescription")}
                scene={selected?.scene ?? "mountain"}
                onClose={() => setSelected(null)}
            />
            <PreviewDialog
                open={creating}
                title={t("rooms.create")}
                description={t("mockup.createRoomDescription")}
                scene="island"
                onClose={() => setCreating(false)}
            />
        </main>
    );
}
