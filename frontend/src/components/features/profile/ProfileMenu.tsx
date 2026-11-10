import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Avatar, Button, Icon, KeyboardShortcut, Tooltip, MenuItem } from "@/components/ui";

type ProfileMenuProps = {
    onLogin: () => void;
    onBrand: () => void;
    onSettings: () => void;
    onProfile: () => void;
    mobile?: boolean;
    member: { nickname: string; email: string | null } | null;
    profileImageUrl?: string | null;
    busy: boolean;
    onLogout: () => void;
};

export function ProfileMenu({
    onLogin,
    onBrand,
    onSettings,
    onProfile,
    mobile = false,
    member,
    profileImageUrl,
    busy,
    onLogout,
}: ProfileMenuProps) {
    const { t } = useTranslation();
    const [open, setOpen] = useState(false);
    const root = useRef<HTMLDivElement>(null);
    const trigger = useRef<HTMLButtonElement>(null);
    const id = useId();

    useEffect(() => {
        if (!open) return;
        const outside = (event: PointerEvent) => {
            if (event.target instanceof Node && !root.current?.contains(event.target))
                setOpen(false);
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [open]);

    function select(action: () => void): void {
        setOpen(false);
        action();
    }

    return (
        <div
            ref={root}
            className={`profile-menu ${mobile ? "profile-menu--mobile" : "desktop-only"}`}
            onBlur={(event) => {
                if (
                    !(event.relatedTarget instanceof Node) ||
                    !event.currentTarget.contains(event.relatedTarget)
                )
                    setOpen(false);
            }}
            onKeyDown={(event) => {
                if (event.key === "Escape") {
                    event.stopPropagation();
                    setOpen(false);
                    trigger.current?.focus();
                }
                if (["ArrowDown", "ArrowUp"].includes(event.key)) {
                    event.preventDefault();
                    setOpen(true);
                    const items = Array.from(
                        root.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') ??
                            [],
                    );
                    const index = items.findIndex((item) => item === document.activeElement);
                    items[
                        (index + (event.key === "ArrowDown" ? 1 : -1) + items.length) % items.length
                    ]?.focus();
                }
            }}
        >
            <Tooltip content={t("common.account")}>
                <Button
                    ref={trigger}
                    variant="ghost"
                    className={`profile-menu__trigger ${mobile ? "mobile-nav__item" : "icon-button"}`}
                    aria-label={t("common.account")}
                    aria-haspopup="menu"
                    aria-expanded={open}
                    aria-controls={open ? id : undefined}
                    onClick={() => setOpen(!open)}
                >
                    {member ? (
                        <Avatar src={profileImageUrl ?? null} />
                    ) : (
                        <span className="profile-menu__anonymous" aria-hidden="true">
                            <img src="/brand/anonymous-profile.svg" alt="" />
                        </span>
                    )}
                </Button>
            </Tooltip>
            {open && (
                <div
                    className="ui-menu-surface profile-menu__panel"
                    id={id}
                    role="menu"
                    aria-label={t("common.account")}
                >
                    {member && (
                        <div className="profile-menu__identity">
                            <Avatar src={profileImageUrl ?? null} />
                            <span>
                                <strong>{member.nickname}</strong>
                                <span>{member.email ?? t("settings.viewProfile")}</span>
                            </span>
                        </div>
                    )}
                    {member && (
                        <MenuItem
                            role="menuitem"
                            className="profile-menu__item"
                            onClick={() => select(onProfile)}
                        >
                            <Icon name="profile" />
                            <span>{t("settings.profile")}</span>
                            <KeyboardShortcut keys={["shift", "mod", "P"]} />
                        </MenuItem>
                    )}
                    <MenuItem
                        type="button"
                        role="menuitem"
                        className="profile-menu__item"
                        onClick={() => select(onSettings)}
                    >
                        <Icon name="settings" />
                        <span>{t("profile.settings")}</span>
                        <KeyboardShortcut keys={["mod", ","]} />
                    </MenuItem>
                    <MenuItem
                        type="button"
                        role="menuitem"
                        className="profile-menu__item"
                        onClick={() => select(onBrand)}
                    >
                        <Icon name="grid" />
                        <span>{t("brand.guide")}</span>
                    </MenuItem>
                    <div className="profile-menu__welcome">
                        {!member && (
                            <>
                                <strong>{t("profile.welcome")}</strong>
                                <p>{t("profile.description")}</p>
                            </>
                        )}
                        <Button
                            variant="brand"
                            role="menuitem"
                            loading={busy}
                            onClick={() => select(member ? onLogout : onLogin)}
                        >
                            {t(member ? "auth.logout" : "profile.login")}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}
