import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";
import { roomPreviews } from "@/data/preview";

export function RoomsPage() {
    const { t } = useTranslation();

    return (
        <main id="main-content" tabIndex={-1} className="workspace-page workspace-page--list">
            <div className="page-heading">
                <h1>{t("rooms.heading")}</h1>
                <Button variant="ghost" className="icon-button" aria-label={t("rooms.create")}>
                    <Icon name="plus" />
                </Button>
            </div>
            <div className="room-list">
                {roomPreviews.map((room) => (
                    <article className="room-row" key={room.id}>
                        <img src={room.image} alt="" />
                        <div className="room-row__body">
                            <h2>{t(room.title)}</h2>
                            <p>{t(room.message)}</p>
                        </div>
                        <Icon name="chevron" />
                    </article>
                ))}
            </div>
        </main>
    );
}
