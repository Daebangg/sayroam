import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Avatar, Button, Icon, TextField } from "@/components/ui";
import type { AvatarPreset } from "@/components/ui";
import { formatTime } from "@/i18n/locale";

type ChatMessage = {
    id: number;
    name: string;
    text: string;
    time: string;
    preset: AvatarPreset | null;
};

type TripChatPanelProps = {
    title: string;
    onClose: () => void;
};

// 채팅 연동 전 배치를 확인하는 정적 화면이다. 보낸 메시지는 이 화면에만 보이고 저장하지 않는다.
export function TripChatPanel({ title, onClose }: TripChatPanelProps) {
    const { t, i18n } = useTranslation();
    const [draft, setDraft] = useState("");
    const [sent, setSent] = useState<ChatMessage[]>([]);
    const samples: ChatMessage[] = [
        {
            id: -1,
            name: t("tripDetail.chatSampleName1"),
            text: t("tripDetail.chatSampleText1"),
            time: t("tripDetail.chatSampleTime1"),
            preset: "traveler",
        },
        {
            id: -2,
            name: t("tripDetail.chatSampleName2"),
            text: t("tripDetail.chatSampleText2"),
            time: t("tripDetail.chatSampleTime2"),
            preset: "sunrise",
        },
    ];
    return (
        <section
            className="trip-side-panel trip-chat"
            aria-labelledby="trip-chat-title"
            onKeyDown={(event) => {
                if (event.key === "Escape") onClose();
            }}
        >
            <header className="trip-side-panel__header">
                <div>
                    <h2 id="trip-chat-title">{title}</h2>
                    <span>{t("tripDetail.chatMembers", { count: samples.length + 1 })}</span>
                </div>
                <Button
                    variant="plain"
                    className="trip-side-panel__close"
                    aria-label={t("tripDetail.chatClose")}
                    onClick={onClose}
                >
                    <Icon name="close" variant="outline" />
                </Button>
            </header>
            <p className="trip-chat__notice">{t("tripDetail.chatPreview")}</p>
            <ol className="trip-chat__messages">
                {[...samples, ...sent].map((message) => (
                    <li
                        key={message.id}
                        className={`trip-chat__message${message.preset ? "" : " trip-chat__message--mine"}`}
                    >
                        {message.preset && <Avatar preset={message.preset} size="small" />}
                        <div>
                            {message.preset && <span>{message.name}</span>}
                            <p>{message.text}</p>
                            <time>{message.time}</time>
                        </div>
                    </li>
                ))}
            </ol>
            <form
                className="trip-chat__form"
                onSubmit={(event) => {
                    event.preventDefault();
                    const text = draft.trim();
                    if (!text) return;
                    const time = formatTime(i18n.resolvedLanguage ?? "ko", new Date());
                    setSent((messages) => [
                        ...messages,
                        {
                            id: messages.length + 1,
                            name: t("tripDetail.chatMe"),
                            text,
                            time,
                            preset: null,
                        },
                    ]);
                    setDraft("");
                }}
            >
                <TextField
                    label={t("tripDetail.chatInputLabel")}
                    placeholder={t("tripDetail.chatInputPlaceholder")}
                    value={draft}
                    onChange={(event) => setDraft(event.currentTarget.value)}
                />
                <Button type="submit" aria-label={t("tripDetail.chatSend")}>
                    <Icon name="arrow" variant="outline" />
                </Button>
            </form>
        </section>
    );
}
