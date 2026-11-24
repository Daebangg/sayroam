import { useLayoutEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Avatar, Button, Icon, TextField } from "@/components/ui";
import type {
    ChatConnection,
    ChatMessage,
    OutgoingMessage,
    RoomMember,
    TripChatStatus,
} from "@/hooks/chat.types";
import { formatTime } from "@/i18n/locale";

// 서버 검증과 같은 기준. 입력 길이를 UTF-16 단위로 막으므로 코드 포인트 기준보다 조금 엄격하다.
const MESSAGE_MAX_LENGTH = 500;
// 맨 아래에서 이만큼 안에 있으면 맨 아래를 보는 중으로 본다.
const BOTTOM_THRESHOLD_PX = 80;

type TripChatPanelProps = {
    title: string;
    myUserId: number;
    hasRoom: boolean;
    status: TripChatStatus;
    connection: ChatConnection;
    messages: ChatMessage[];
    outgoing: OutgoingMessage[];
    members: RoomMember[];
    onSend: (content: string) => void;
    onRetry: (clientMessageId: string) => void;
    onDiscard: (clientMessageId: string) => void;
    onReload: () => void;
    onInvite: () => void;
    onClose: () => void;
};

export function TripChatPanel({
    title,
    myUserId,
    hasRoom,
    status,
    connection,
    messages,
    outgoing,
    members,
    onSend,
    onRetry,
    onDiscard,
    onReload,
    onInvite,
    onClose,
}: TripChatPanelProps) {
    const { t, i18n } = useTranslation();
    const language = i18n.resolvedLanguage ?? "ko";
    const [draft, setDraft] = useState("");
    const list = useRef<HTMLOListElement>(null);
    // 처음 열면 맨 아래부터 보여 준다. 위로 올려 지난 메시지를 보는 중이면 새 메시지가 와도 위치를 지킨다.
    const atBottom = useRef(true);
    const justSent = useRef(false);
    const byId = useMemo(
        () => new Map(members.map((member) => [member.userId, member])),
        [members],
    );
    const joinedCount = members.filter((member) => member.status === "JOINED").length;
    const connected = connection === "connected";
    const text = draft.trim();

    useLayoutEffect(() => {
        const element = list.current;
        if (!element || !(atBottom.current || justSent.current)) return;
        element.scrollTop = element.scrollHeight;
        atBottom.current = true;
        justSent.current = false;
    }, [messages, outgoing, status]);

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
                    {hasRoom && status === "ready" && (
                        <span>{t("tripDetail.chatMembers", { count: joinedCount })}</span>
                    )}
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
            {!hasRoom ? (
                <div className="trip-chat__state">
                    <p>{t("tripDetail.chatNoRoom")}</p>
                    <Button variant="outline" onClick={onInvite}>
                        {t("tripDetail.chatInvite")}
                    </Button>
                </div>
            ) : (
                <>
                    {connection === "reconnecting" && (
                        <p className="trip-chat__notice" role="status">
                            {t("tripDetail.chatReconnecting")}
                        </p>
                    )}
                    {connection === "closed" && (
                        <p className="trip-chat__notice trip-chat__notice--error" role="alert">
                            {t("tripDetail.chatClosed")}
                        </p>
                    )}
                    {status === "failed" ? (
                        <div className="trip-chat__state">
                            <p>{t("tripDetail.chatLoadFailed")}</p>
                            <Button variant="outline" onClick={onReload}>
                                {t("tripDetail.retry")}
                            </Button>
                        </div>
                    ) : status === "loading" ? (
                        <p className="trip-chat__state" role="status">
                            {t("tripDetail.chatLoading")}
                        </p>
                    ) : (
                        <ol
                            ref={list}
                            className="trip-chat__messages"
                            aria-live="polite"
                            onScroll={(event) => {
                                const element = event.currentTarget;
                                atBottom.current =
                                    element.scrollHeight -
                                        element.scrollTop -
                                        element.clientHeight <=
                                    BOTTOM_THRESHOLD_PX;
                            }}
                        >
                            {messages.length === 0 && outgoing.length === 0 && (
                                <li className="trip-chat__empty">{t("tripDetail.chatEmpty")}</li>
                            )}
                            {messages.map((message) => {
                                const time = (
                                    <time dateTime={message.createdAt}>
                                        {formatTime(language, new Date(message.createdAt))}
                                    </time>
                                );
                                if (message.senderId === null)
                                    return (
                                        <li
                                            key={message.messageId}
                                            className="trip-chat__message trip-chat__message--system"
                                        >
                                            <p>{message.content}</p>
                                        </li>
                                    );
                                if (message.senderId === myUserId)
                                    return (
                                        <li
                                            key={message.messageId}
                                            className="trip-chat__message trip-chat__message--mine"
                                        >
                                            <div className="trip-chat__body">
                                                <div className="trip-chat__bubble">
                                                    <p>{message.content}</p>
                                                    {time}
                                                </div>
                                            </div>
                                        </li>
                                    );
                                const sender = byId.get(message.senderId);
                                return (
                                    <li key={message.messageId} className="trip-chat__message">
                                        <Avatar src={sender?.profileImageUrl ?? null} />
                                        <div className="trip-chat__body">
                                            <span className="trip-chat__sender">
                                                {sender?.nickname ??
                                                    t("tripDetail.chatUnknownMember")}
                                            </span>
                                            <div className="trip-chat__bubble">
                                                <p>{message.content}</p>
                                                {time}
                                            </div>
                                        </div>
                                    </li>
                                );
                            })}
                            {outgoing.map((message) => (
                                <li
                                    key={message.clientMessageId}
                                    className={`trip-chat__message trip-chat__message--mine trip-chat__message--${message.status}`}
                                >
                                    <div className="trip-chat__body">
                                        <div className="trip-chat__bubble">
                                            <p>{message.content}</p>
                                            {message.status === "sending" && (
                                                <span>{t("tripDetail.chatSending")}</span>
                                            )}
                                        </div>
                                        {message.status === "failed" && (
                                            <span className="trip-chat__failed">
                                                {t("tripDetail.chatSendFailed")}
                                                <Button
                                                    variant="plain"
                                                    onClick={() => onRetry(message.clientMessageId)}
                                                    disabled={!connected}
                                                >
                                                    {t("tripDetail.chatResend")}
                                                </Button>
                                                <Button
                                                    variant="plain"
                                                    onClick={() =>
                                                        onDiscard(message.clientMessageId)
                                                    }
                                                >
                                                    {t("tripDetail.chatDiscard")}
                                                </Button>
                                            </span>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ol>
                    )}
                    <form
                        className="trip-chat__form"
                        onSubmit={(event) => {
                            event.preventDefault();
                            if (!text || !connected) return;
                            justSent.current = true;
                            onSend(text);
                            setDraft("");
                        }}
                    >
                        <TextField
                            label={t("tripDetail.chatInputLabel")}
                            placeholder={t("tripDetail.chatInputPlaceholder")}
                            value={draft}
                            maxLength={MESSAGE_MAX_LENGTH}
                            onChange={(event) => setDraft(event.currentTarget.value)}
                        />
                        <Button
                            type="submit"
                            aria-label={t("tripDetail.chatSend")}
                            disabled={!text || !connected}
                        >
                            <Icon name="arrow" variant="outline" />
                        </Button>
                    </form>
                </>
            )}
        </section>
    );
}
