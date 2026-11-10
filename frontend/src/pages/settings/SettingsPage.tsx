import { Link, Outlet, useLocation, useOutletContext } from "react-router";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { useAccountAction } from "@/hooks/api/auth/useAccountAction";
import { useLoginMethods } from "@/hooks/api/auth/useLoginMethods";
import { useNicknameEditor } from "@/hooks/api/auth/useNicknameEditor";
import type { AccountPurpose } from "@/hooks/account.types";
import { AccountActionDialog } from "@/components/features/settings/AccountDialogs";
import { Avatar, Button, ButtonLink, Icon } from "@/components/ui";
import {
    AccountDetails,
    AppearanceSettings,
    ProfileDetails,
    SettingsHeading,
    SettingsRow,
} from "@/components/features/settings/SettingsContent";
import type { PageContext } from "@/routing/context";
import { pagePaths } from "@/routing/paths";

const sections = [
    { id: "general", icon: "settings", to: pagePaths.settings },
    { id: "appearance", icon: "sun", to: pagePaths.appearance },
    { id: "account", icon: "profile", to: pagePaths.account },
] as const;

export function SettingsPage() {
    const context = useOutletContext<PageContext>();
    const { t } = useTranslation();
    const { pathname } = useLocation();
    const currentPath = pathname.replace(/\/+$/, "").toLowerCase();
    return (
        <main id="main-content" tabIndex={-1} className="settings-page">
            <div className="settings-landscape" aria-hidden="true" />
            <div className="settings-layout">
                <aside className="settings-sidebar">
                    <div className="settings-sidebar__cover">
                        <img src="/images/travel-coast.jpg" alt="" />
                    </div>
                    <p className="settings-sidebar__title">{t("settings.signature")}</p>
                    <ButtonLink to={pagePaths.plaza} variant="plain" className="settings-back">
                        <Icon name="arrow" variant="outline" />
                        {t("settings.backToApp")}
                    </ButtonLink>
                    <nav className="settings-navigation" aria-label={t("settings.navigation")}>
                        {sections.map((section) => {
                            const active =
                                currentPath === section.to ||
                                (section.id === "account" && currentPath === pagePaths.profile);
                            return (
                                <Link
                                    key={section.id}
                                    to={section.to}
                                    aria-current={active ? "page" : undefined}
                                    className={`settings-navigation__item ${active ? "is-active" : ""}`}
                                >
                                    <Icon name={section.icon} />
                                    {t(`settings.${section.id}`)}
                                    <Icon name="chevron" variant="outline" />
                                </Link>
                            );
                        })}
                    </nav>
                    <ButtonLink
                        to={pagePaths.brand}
                        variant="plain"
                        className="settings-sidebar__guide"
                    >
                        <Icon name="guide" />
                        {t("brand.guide")}
                    </ButtonLink>
                </aside>
                <div className="settings-content">
                    <Outlet context={context} />
                </div>
            </div>
        </main>
    );
}

export function GeneralSettingsPage() {
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const { t } = useTranslation();
    return (
        <>
            <SettingsHeading title={t("settings.general")} />
            <div className="settings-identity">
                {member ? (
                    <Avatar src={member.profileImageUrl} size="large" />
                ) : (
                    <span className="settings-identity__anonymous">
                        <img src="/brand/anonymous-profile.svg" alt="" />
                    </span>
                )}
                <div>
                    <strong>{member ? member.nickname : t("auth.requiredTitle")}</strong>
                    {member?.email && <p>{member.email}</p>}
                </div>
                {!member && (
                    <Button variant="brand" onClick={onOpenAuth}>
                        {t("auth.login")}
                    </Button>
                )}
            </div>
            <SettingsRow label={t("settings.account")} to={pagePaths.account}>
                <Icon name="profile" />
            </SettingsRow>
            <SettingsRow label={t("settings.appearance")} to={pagePaths.appearance}>
                <Icon name="sun" />
            </SettingsRow>
        </>
    );
}

export function AccountSettingsPage() {
    const { member, onUpdateNickname } = useOutletContext<PageContext>();
    const nicknameEditor = useNicknameEditor(onUpdateNickname);
    const { t } = useTranslation();
    const { methods, failed, reload } = useLoginMethods(member?.userId ?? 0);
    const [action, setAction] = useState<AccountPurpose | null>(null);
    return member ? (
        <>
            <SettingsHeading title={t("settings.account")} />
            <AccountDetails
                member={member}
                methods={methods}
                methodsFailed={failed}
                onReloadMethods={reload}
                nicknameEditor={nicknameEditor}
                onAction={setAction}
            />
            {action && (
                <AccountActionFlow
                    key={action}
                    purpose={action}
                    email={member.email}
                    onClose={() => setAction(null)}
                />
            )}
        </>
    ) : null;
}

export function ProfileSettingsPage() {
    const { member, onUpdateNickname } = useOutletContext<PageContext>();
    const { t } = useTranslation();
    const nicknameEditor = useNicknameEditor(onUpdateNickname);
    return member ? (
        <>
            <ButtonLink
                to={pagePaths.account}
                variant="plain"
                className="settings-back settings-back--content"
            >
                <Icon name="arrow" variant="outline" />
                {t("settings.backToAccount")}
            </ButtonLink>
            <SettingsHeading title={t("settings.profile")} />
            <ProfileDetails member={member} nicknameEditor={nicknameEditor} />
        </>
    ) : null;
}

function AccountActionFlow({
    purpose,
    email,
    onClose,
}: {
    purpose: AccountPurpose;
    email: string | null;
    onClose: () => void;
}) {
    const { onAccountResult } = useOutletContext<PageContext>();
    const flow = useAccountAction(purpose, onAccountResult);
    return (
        <AccountActionDialog
            {...flow}
            purpose={purpose}
            email={email}
            onRequest={flow.requestCode}
            onVerify={flow.verifyCode}
            onComplete={flow.complete}
            onClose={onClose}
        />
    );
}

export function AppearanceSettingsPage() {
    const { themePreference, onThemeChange, onSelectLanguage } = useOutletContext<PageContext>();
    const { t } = useTranslation();
    return (
        <>
            <SettingsHeading title={t("settings.appearance")} />
            <AppearanceSettings
                theme={themePreference}
                onThemeChange={onThemeChange}
                onLanguageChange={onSelectLanguage}
            />
        </>
    );
}
