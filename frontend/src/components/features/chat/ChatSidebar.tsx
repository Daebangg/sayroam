import {
    useEffect,
    useLayoutEffect,
    useRef,
    type Dispatch,
    type RefObject,
    type SetStateAction,
} from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, ModalCloseButton, TextField, Tooltip } from "@/components/ui";
import { openChatShortcut } from "@/data/shortcuts";

type ChatSidebarProps = {
    id: string;
    open: boolean;
    setOpen: Dispatch<SetStateAction<boolean>>;
    trigger: RefObject<HTMLButtonElement | null>;
};

export function ChatSidebarButton({ id, open, setOpen, trigger }: ChatSidebarProps) {
    const { t } = useTranslation();
    const label = t("navigation.chat");
    return (
        <Tooltip content={label} shortcut={openChatShortcut} touchHint>
            <Button
                ref={trigger}
                variant="plain"
                className={`nav-item${open ? " is-active" : ""}`}
                aria-label={label}
                aria-controls={id}
                aria-expanded={open}
                aria-keyshortcuts="Alt+C"
                onClick={() => setOpen((value) => !value)}
            >
                <Icon name="conversation" />
                <span>{label}</span>
            </Button>
        </Tooltip>
    );
}

export function ChatSidebar({ id, open, setOpen, trigger }: ChatSidebarProps) {
    const { t } = useTranslation();
    const panel = useRef<HTMLElement>(null);
    useLayoutEffect(() => {
        if (open) panel.current?.focus({ preventScroll: true });
    }, [open]);

    useEffect(() => {
        function shortcut(event: KeyboardEvent) {
            if (
                event.defaultPrevented ||
                event.repeat ||
                event.isComposing ||
                event.code !== "KeyC" ||
                !event.altKey ||
                event.ctrlKey ||
                event.metaKey ||
                event.shiftKey
            )
                return;
            if (
                document.querySelector("dialog:modal, [aria-modal='true']") ||
                (event.target instanceof HTMLElement &&
                    (event.target.isContentEditable ||
                        event.target.closest("input, textarea, select")))
            )
                return;
            event.preventDefault();
            trigger.current?.focus({ preventScroll: true });
            setOpen((value) => !value);
        }
        window.addEventListener("keydown", shortcut);
        return () => window.removeEventListener("keydown", shortcut);
    }, [setOpen, trigger]);

    function close() {
        setOpen(false);
        trigger.current?.focus({ preventScroll: true });
    }

    return (
        <aside
            ref={panel}
            id={id}
            className="edge-panel chat-sidebar"
            hidden={!open}
            inert={!open}
            tabIndex={-1}
            aria-labelledby={`${id}-title`}
            onKeyDown={(event) => {
                if (event.key === "Escape" && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    event.stopPropagation();
                    close();
                }
            }}
        >
            <header className="chat-sidebar__header">
                <h2 id={`${id}-title`}>{t("navigation.chat")}</h2>
                <ModalCloseButton inline onClose={close} />
            </header>
            <div className="chat-sidebar__composer">
                <TextField
                    label={t("tripDetail.chatInputLabel")}
                    placeholder={t("tripDetail.chatInputPlaceholder")}
                    autoComplete="off"
                />
            </div>
        </aside>
    );
}
