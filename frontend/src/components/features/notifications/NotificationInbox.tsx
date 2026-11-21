import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, NotificationBadge, Spinner, Tooltip, type IconName } from "@/components/ui";
import type { NotificationSummary } from "@/hooks/notification.types";

export type InboxItem = {
    id: string;
    title: string;
    content: string;
    context?: string;
    time: string;
    section: "today" | "earlier";
    icon: IconName;
    isRead: boolean;
};

type NotificationInboxProps = {
    items: readonly InboxItem[];
    inline?: boolean;
    summary?: NotificationSummary | null;
    arrivalVersion?: number;
    loading?: boolean;
    loaded?: boolean;
    busy?: boolean;
    error?: "load" | "change" | null;
    hasMore?: boolean;
    loadingMore?: boolean;
    onOpenChange?: (open: boolean) => void;
    onFilterChange?: (unreadOnly: boolean) => void;
    onRetry?: () => void;
    onLoadMore?: () => void;
    onReadChange?: (id: string, isRead: boolean) => void;
    onReadAll?: () => void;
    onDelete?: (id: string) => void;
};

export function NotificationInbox({
    items,
    inline = false,
    summary,
    arrivalVersion,
    loading = false,
    loaded = true,
    busy = false,
    error = null,
    hasMore = false,
    loadingMore = false,
    onOpenChange,
    onFilterChange,
    onRetry,
    onLoadMore,
    onReadChange,
    onReadAll,
    onDelete,
}: NotificationInboxProps) {
    const { t } = useTranslation();
    const [open, setOpen] = useState(false);
    const [unreadOnly, setUnreadOnly] = useState(false);
    const [expandedId, setExpandedId] = useState<string | null>(null);
    const root = useRef<HTMLDivElement>(null);
    const trigger = useRef<HTMLButtonElement>(null);
    const panel = useRef<HTMLDivElement>(null);
    const bell = useRef<HTMLSpanElement>(null);
    const ring = useRef<Animation | null>(null);
    const knownIds = useRef(new Set(items.map((item) => item.id)));
    const lastArrival = useRef(arrivalVersion);
    const id = useId();
    const unreadCount = summary?.unreadCount ?? items.filter((item) => !item.isRead).length;
    const visibleItems = unreadOnly
        ? items.filter((item) => !item.isRead || item.id === expandedId)
        : items;

    useEffect(() => {
        const hasNewUnread =
            arrivalVersion === undefined
                ? items.some((item) => !item.isRead && !knownIds.current.has(item.id))
                : arrivalVersion > (lastArrival.current ?? arrivalVersion);
        lastArrival.current = arrivalVersion;
        items.forEach((item) => knownIds.current.add(item.id));
        if (
            !hasNewUnread ||
            window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
            ring.current?.playState === "running"
        )
            return;
        ring.current?.cancel();
        ring.current =
            bell.current?.animate(
                [
                    { transform: "rotate(0deg)", offset: 0 },
                    { transform: "rotate(-14deg)", offset: 0.12 },
                    { transform: "rotate(12deg)", offset: 0.27 },
                    { transform: "rotate(-9deg)", offset: 0.42 },
                    { transform: "rotate(6deg)", offset: 0.57 },
                    { transform: "rotate(-3deg)", offset: 0.72 },
                    { transform: "rotate(1.5deg)", offset: 0.86 },
                    { transform: "rotate(0deg)", offset: 1 },
                ],
                { duration: 1300, easing: "ease-in-out" },
            ) ?? null;
    }, [items, arrivalVersion]);

    useEffect(() => {
        onOpenChange?.(open);
    }, [open, onOpenChange]);
    useEffect(() => () => onOpenChange?.(false), [onOpenChange]);

    useEffect(() => {
        const preference = window.matchMedia("(prefers-reduced-motion: reduce)");
        const stopForReducedMotion = () => {
            if (preference.matches) ring.current?.cancel();
        };
        preference.addEventListener("change", stopForReducedMotion);
        return () => {
            preference.removeEventListener("change", stopForReducedMotion);
            ring.current?.cancel();
        };
    }, []);

    useEffect(() => {
        if (!open || inline) return;
        panel.current?.focus({ preventScroll: true });
        const outside = (event: PointerEvent) => {
            if (event.target instanceof Node && !root.current?.contains(event.target)) {
                setOpen(false);
            }
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [open, inline]);

    function close(): void {
        setOpen(false);
        trigger.current?.focus();
    }

    return (
        <div
            className={`notification-inbox${inline ? " notification-inbox--inline" : ""}`}
            ref={root}
            onBlur={(event) => {
                if (
                    !inline &&
                    event.relatedTarget instanceof Node &&
                    !event.currentTarget.contains(event.relatedTarget)
                ) {
                    setOpen(false);
                }
            }}
            onKeyDown={(event) => {
                if (event.key === "Escape" && open && !inline) {
                    event.preventDefault();
                    event.stopPropagation();
                    close();
                }
            }}
        >
            {!inline && (
                <Tooltip content={t("common.notifications")}>
                    <Button
                        ref={trigger}
                        variant="ghost"
                        className="icon-button notification-trigger"
                        data-unread={unreadCount > 0 || undefined}
                        aria-label={t("inbox.trigger", { count: unreadCount })}
                        aria-haspopup="dialog"
                        aria-expanded={open}
                        aria-controls={open ? id : undefined}
                        onClick={() => {
                            ring.current?.cancel();
                            setOpen((current) => !current);
                        }}
                    >
                        <span ref={bell} className="notification-trigger__bell" aria-hidden="true">
                            <Icon name="bell" />
                        </span>
                        {unreadCount > 0 && (
                            <span className="notification-trigger__badge" aria-hidden="true">
                                <NotificationBadge
                                    count={unreadCount}
                                    maxCount={99}
                                    label={t("inbox.trigger", { count: unreadCount })}
                                />
                            </span>
                        )}
                    </Button>
                </Tooltip>
            )}
            {(inline || open) && (
                <div
                    ref={panel}
                    id={id}
                    className="notification-inbox__panel ui-menu-surface"
                    role={inline ? "region" : "dialog"}
                    aria-labelledby={`${id}-title`}
                    tabIndex={-1}
                >
                    <header className="notification-inbox__header">
                        <div className="notification-inbox__heading">
                            <h2 id={`${id}-title`}>{t("common.notifications")}</h2>
                        </div>
                        <div className="notification-inbox__tools">
                            <Button
                                variant="plain"
                                className="notification-inbox__read-all"
                                disabled={busy || unreadCount === 0 || !onReadAll}
                                onClick={() => {
                                    panel.current?.focus({ preventScroll: true });
                                    setExpandedId(null);
                                    onReadAll?.();
                                }}
                            >
                                {t("inbox.readAll")}
                            </Button>
                            {!inline && (
                                <Tooltip content={t("inbox.close")} shortcut={["Escape"]}>
                                    <Button
                                        variant="ghost"
                                        className="icon-button notification-inbox__close"
                                        aria-label={t("inbox.close")}
                                        onClick={close}
                                    >
                                        <Icon name="close" variant="outline" />
                                    </Button>
                                </Tooltip>
                            )}
                        </div>
                    </header>
                    <div
                        className="notification-inbox__filters"
                        role="group"
                        aria-label={t("inbox.filter")}
                    >
                        <Button
                            variant="plain"
                            className="notification-inbox__filter"
                            aria-pressed={!unreadOnly}
                            onClick={() => {
                                setExpandedId(null);
                                setUnreadOnly(false);
                                onFilterChange?.(false);
                            }}
                        >
                            {t("inbox.all")}
                            <span className="notification-inbox__count">
                                {summary?.totalCount ?? items.length}
                            </span>
                        </Button>
                        <Button
                            variant="plain"
                            className="notification-inbox__filter"
                            aria-pressed={unreadOnly}
                            onClick={() => {
                                setExpandedId(null);
                                setUnreadOnly(true);
                                onFilterChange?.(true);
                            }}
                        >
                            {t("inbox.unread")}
                            <span className="notification-inbox__count">{unreadCount}</span>
                        </Button>
                    </div>
                    {error && (
                        <div className="notification-inbox__notice" role="alert">
                            <span>
                                {t(error === "load" ? "inbox.loadError" : "inbox.changeError")}
                            </span>
                            <Button
                                variant="plain"
                                className="notification-inbox__read-all"
                                onClick={onRetry}
                                disabled={loading || busy}
                            >
                                {t("inbox.retry")}
                            </Button>
                        </div>
                    )}
                    <div className="notification-inbox__scroll" aria-busy={loading || loadingMore}>
                        <div
                            className="notification-inbox__results"
                            data-direction={unreadOnly ? "next" : "previous"}
                            key={`${unreadOnly}-${loaded}`}
                        >
                            {loading && !loaded ? (
                                <div className="notification-inbox__loading">
                                    <Spinner label={t("inbox.loading")} />
                                </div>
                            ) : !loaded ? null : visibleItems.length === 0 ? (
                                <div className="notification-inbox__empty" role="status">
                                    <span className="notification-inbox__empty-icon">
                                        <Icon name={unreadOnly ? "check" : "mail"} />
                                    </span>
                                    <h3>{t(unreadOnly ? "inbox.caughtUp" : "inbox.emptyTitle")}</h3>
                                </div>
                            ) : (
                                (["today", "earlier"] as const).map((section) => {
                                    const sectionItems = visibleItems.filter(
                                        (item) => item.section === section,
                                    );
                                    if (sectionItems.length === 0) return null;
                                    return (
                                        <section
                                            className="notification-inbox__section"
                                            key={section}
                                        >
                                            <h3>{t(`inbox.${section}`)}</h3>
                                            <ul>
                                                {sectionItems.map((item) => {
                                                    const expanded = expandedId === item.id;
                                                    return (
                                                        <li
                                                            key={item.id}
                                                            className={`notification-inbox__item${!item.isRead ? " is-unread" : ""}${expanded ? " is-expanded" : ""}`}
                                                        >
                                                            <span
                                                                className="notification-inbox__item-icon"
                                                                data-icon={item.icon}
                                                            >
                                                                <Icon name={item.icon} />
                                                            </span>
                                                            <div className="notification-inbox__item-body">
                                                                <Button
                                                                    variant="plain"
                                                                    className="notification-inbox__message"
                                                                    aria-expanded={expanded}
                                                                    onClick={() => {
                                                                        setExpandedId(
                                                                            expanded
                                                                                ? null
                                                                                : item.id,
                                                                        );
                                                                        if (
                                                                            !expanded &&
                                                                            !item.isRead &&
                                                                            !busy
                                                                        )
                                                                            onReadChange?.(
                                                                                item.id,
                                                                                true,
                                                                            );
                                                                    }}
                                                                >
                                                                    <span className="notification-inbox__item-title">
                                                                        {item.title}
                                                                        {!item.isRead && (
                                                                            <span
                                                                                className="notification-inbox__unread-dot"
                                                                                aria-label={t(
                                                                                    "inbox.unread",
                                                                                )}
                                                                            />
                                                                        )}
                                                                    </span>
                                                                    <span className="notification-inbox__content">
                                                                        {item.content}
                                                                    </span>
                                                                    <span className="notification-inbox__meta">
                                                                        {item.context && (
                                                                            <span>
                                                                                {item.context}
                                                                            </span>
                                                                        )}
                                                                        <span>{item.time}</span>
                                                                    </span>
                                                                </Button>
                                                                <div className="notification-inbox__item-actions">
                                                                    <Tooltip
                                                                        content={t(
                                                                            item.isRead
                                                                                ? "inbox.markUnread"
                                                                                : "inbox.markRead",
                                                                        )}
                                                                    >
                                                                        <Button
                                                                            variant="plain"
                                                                            className="notification-inbox__action"
                                                                            aria-label={t(
                                                                                item.isRead
                                                                                    ? "inbox.markUnread"
                                                                                    : "inbox.markRead",
                                                                            )}
                                                                            disabled={
                                                                                busy ||
                                                                                !onReadChange
                                                                            }
                                                                            onClick={() => {
                                                                                panel.current?.focus(
                                                                                    {
                                                                                        preventScroll: true,
                                                                                    },
                                                                                );
                                                                                onReadChange?.(
                                                                                    item.id,
                                                                                    !item.isRead,
                                                                                );
                                                                                setExpandedId(null);
                                                                            }}
                                                                        >
                                                                            <Icon
                                                                                name={
                                                                                    item.isRead
                                                                                        ? "mail"
                                                                                        : "check"
                                                                                }
                                                                                variant="outline"
                                                                            />
                                                                        </Button>
                                                                    </Tooltip>
                                                                    <Tooltip
                                                                        content={t("inbox.delete")}
                                                                    >
                                                                        <Button
                                                                            variant="plain"
                                                                            className="notification-inbox__action notification-inbox__delete"
                                                                            aria-label={t(
                                                                                "inbox.delete",
                                                                            )}
                                                                            disabled={
                                                                                busy || !onDelete
                                                                            }
                                                                            onClick={() => {
                                                                                panel.current?.focus(
                                                                                    {
                                                                                        preventScroll: true,
                                                                                    },
                                                                                );
                                                                                setExpandedId(null);
                                                                                onDelete?.(item.id);
                                                                            }}
                                                                        >
                                                                            <Icon
                                                                                name="close"
                                                                                variant="outline"
                                                                            />
                                                                        </Button>
                                                                    </Tooltip>
                                                                </div>
                                                            </div>
                                                        </li>
                                                    );
                                                })}
                                            </ul>
                                        </section>
                                    );
                                })
                            )}
                            {hasMore && (
                                <div className="notification-inbox__more">
                                    <Button
                                        variant="plain"
                                        className="notification-inbox__read-all"
                                        disabled={loading || loadingMore || busy}
                                        onClick={onLoadMore}
                                    >
                                        {t(loadingMore ? "inbox.loading" : "inbox.loadMore")}
                                    </Button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
