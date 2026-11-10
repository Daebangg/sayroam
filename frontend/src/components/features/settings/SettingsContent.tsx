import { useEffect, useRef, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import {
    Avatar,
    Button,
    ButtonLink,
    DropdownMenu,
    Icon,
    TextField,
    Tooltip,
} from "@/components/ui";
import type { AuthState } from "@/hooks/auth.types";
import type { ThemePreference } from "@/hooks/useTheme";
import { normalizeLanguage, type Language } from "@/i18n/locale";
import { pagePaths } from "@/routing/paths";
import type { AccountPurpose, LoginMethods, NicknameEditor } from "@/hooks/account.types";

type Member = NonNullable<AuthState["user"]>;

export function SettingsHeading({ title }: { title: string }) {
    return (
        <header className="settings-heading">
            <h1>{title}</h1>
        </header>
    );
}

type SettingsRowProps = {
    label: string;
    description?: string;
    to?: string;
    children?: ReactNode;
    danger?: boolean;
};

export function SettingsRow({
    label,
    description,
    to,
    children,
    danger = false,
}: SettingsRowProps) {
    const className = `settings-row ${danger ? "settings-row--danger" : ""}`;
    const content = (
        <>
            <span className="settings-row__copy">
                <strong>{label}</strong>
                {description && <span>{description}</span>}
            </span>
            <span className="settings-row__value">
                {children}
                {to && <Icon name="chevron" variant="outline" />}
            </span>
        </>
    );
    return to ? (
        <ButtonLink to={to} variant="plain" className={`${className} settings-row--link`}>
            {content}
        </ButtonLink>
    ) : (
        <div className={className}>{content}</div>
    );
}

type AccountDetailsProps = {
    member: Member;
    methods: LoginMethods | null;
    methodsFailed: boolean;
    onReloadMethods: () => void;
    nicknameEditor: NicknameEditor;
    onAction: (purpose: AccountPurpose) => void;
};
export function AccountDetails({
    member,
    methods,
    methodsFailed,
    onReloadMethods,
    nicknameEditor,
    onAction,
}: AccountDetailsProps) {
    const { t } = useTranslation();
    const socialOnly = methods !== null && !methods.passwordEnabled;
    const passwordButton = (
        <Button
            variant="outline"
            disabled={!methods}
            aria-disabled={socialOnly || undefined}
            onClick={() => {
                if (methods?.passwordEnabled) onAction("PASSWORD_CHANGE");
            }}
        >
            {t("settings.changePassword")}
        </Button>
    );
    return (
        <div className="settings-details">
            <section aria-labelledby="account-information">
                <h2 className="settings-section-title" id="account-information">
                    {t("settings.basicInformation")}
                </h2>
                <SettingsRow label={t("settings.nickname")}>
                    <InlineNickname nickname={member.nickname} editor={nicknameEditor} />
                </SettingsRow>
                <SettingsRow label={t("settings.email")}>
                    <span>{member.email ?? t("settings.noEmail")}</span>
                </SettingsRow>
                <SettingsRow label={t("settings.profile")} to={pagePaths.profile}>
                    <Avatar src={member.profileImageUrl} size="small" />
                    <span>{t("settings.viewProfile")}</span>
                </SettingsRow>
            </section>
            <section aria-labelledby="account-security">
                <h2 className="settings-section-title" id="account-security">
                    {t("settings.security")}
                </h2>
                <SettingsRow label={t("accountAction.connectedOAuth")}>
                    {methodsFailed ? (
                        <>
                            <span className="settings-inline-error" role="alert">
                                {t("accountAction.methodsFailed")}
                            </span>
                            <Button variant="outline" onClick={onReloadMethods}>
                                {t("accountAction.retry")}
                            </Button>
                        </>
                    ) : !methods ? (
                        <span role="status">{t("accountAction.loading")}</span>
                    ) : methods.socialProviders.length === 0 ? (
                        <span>{t("accountAction.noneConnected")}</span>
                    ) : (
                        methods.socialProviders.map((provider) => (
                            <span
                                key={provider}
                                className="settings-provider"
                                data-provider={provider}
                            >
                                <img
                                    src={`/brand/providers/${provider === "GOOGLE" ? "google.png" : provider === "KAKAO" ? "kakao.svg" : "naver.png"}`}
                                    alt=""
                                />
                                <span>{t(`accountAction.providers.${provider}`)}</span>
                            </span>
                        ))
                    )}
                </SettingsRow>
                <SettingsRow label={t("settings.changePassword")}>
                    {socialOnly ? (
                        <Tooltip content={t("accountAction.socialPasswordHint")}>
                            {passwordButton}
                        </Tooltip>
                    ) : (
                        passwordButton
                    )}
                </SettingsRow>
                <SettingsRow label={t("settings.deleteAccount")} danger>
                    <Button
                        variant="danger"
                        disabled={!member.email}
                        onClick={() => onAction("ACCOUNT_DELETION")}
                    >
                        {t("settings.deleteAccount")}
                    </Button>
                </SettingsRow>
            </section>
        </div>
    );
}

export function ProfileDetails({
    member,
    nicknameEditor,
}: {
    member: Member;
    nicknameEditor: NicknameEditor;
}) {
    const { t } = useTranslation();
    const gender = member.gender
        ? t(`settings.genderValues.${member.gender}`)
        : t("settings.unset");
    return (
        <div className="settings-details">
            <div className="settings-profile">
                <Avatar src={member.profileImageUrl} size="large" />
                <div>
                    <strong>{member.nickname}</strong>
                </div>
            </div>
            <SettingsRow label={t("settings.nickname")}>
                <InlineNickname nickname={member.nickname} editor={nicknameEditor} />
            </SettingsRow>
            <SettingsRow label={t("settings.gender")}>
                <span>{gender}</span>
            </SettingsRow>
            <SettingsRow label={t("settings.birthYear")}>
                <span>
                    {member.birthYear
                        ? t("settings.birthYearValue", { year: member.birthYear })
                        : t("settings.unset")}
                </span>
            </SettingsRow>
        </div>
    );
}

type AppearanceSettingsProps = {
    theme: ThemePreference;
    onThemeChange: (preference: ThemePreference) => void;
    onLanguageChange: (language: Language) => void;
};

export function AppearanceSettings({
    theme,
    onThemeChange,
    onLanguageChange,
}: AppearanceSettingsProps) {
    const { t, i18n } = useTranslation();
    return (
        <div className="settings-details">
            <section className="settings-theme" aria-labelledby="settings-theme-title">
                <h2 className="settings-section-title" id="settings-theme-title">
                    {t("profile.theme")}
                </h2>
                <div className="settings-theme__options">
                    {(["light", "dark", "system"] as const).map((option) => (
                        <Button
                            key={option}
                            variant="plain"
                            className="settings-theme__option"
                            aria-pressed={theme === option}
                            onClick={() => onThemeChange(option)}
                        >
                            <span
                                className={`settings-theme__preview settings-theme__preview--${option}`}
                                aria-hidden="true"
                            >
                                <span />
                                <span />
                                <span />
                            </span>
                            <span className="settings-theme__label">
                                {t(`profile.${option}`)}
                                <span className="settings-theme__check" aria-hidden="true">
                                    {theme === option && <Icon name="check" variant="outline" />}
                                </span>
                            </span>
                        </Button>
                    ))}
                </div>
            </section>
            <SettingsRow label={t("profile.language")}>
                <DropdownMenu
                    label={t("profile.language")}
                    value={i18n.resolvedLanguage ?? "ko"}
                    options={[
                        { value: "ko", label: t("profile.korean") },
                        { value: "en", label: t("profile.english") },
                    ]}
                    onChange={(language) => onLanguageChange(normalizeLanguage(language))}
                />
            </SettingsRow>
        </div>
    );
}

function InlineNickname({ nickname, editor }: { nickname: string; editor: NicknameEditor }) {
    const { t } = useTranslation();
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(nickname);
    const trigger = useRef<HTMLButtonElement>(null);
    const restoreFocus = useRef(false);
    useEffect(() => {
        if (!editing && restoreFocus.current) {
            trigger.current?.focus();
            restoreFocus.current = false;
        }
    }, [editing]);
    function close(): void {
        if (editor.busy) return;
        restoreFocus.current = true;
        setEditing(false);
    }
    return editing ? (
        <form
            className="settings-nickname-editor"
            onKeyDown={(event) => {
                if (event.key === "Escape") {
                    event.preventDefault();
                    event.stopPropagation();
                    close();
                }
            }}
            onSubmit={(event) => {
                event.preventDefault();
                if (editor.busy) return;
                if (draft.trim() === nickname) {
                    close();
                    return;
                }
                void editor.save(draft).then((saved) => {
                    if (saved) {
                        restoreFocus.current = true;
                        setEditing(false);
                    }
                });
            }}
        >
            <TextField
                label={t("settings.nickname")}
                value={draft}
                autoFocus
                autoComplete="nickname"
                maxLength={50}
                required
                disabled={editor.busy}
                onChange={(event) => setDraft(event.target.value)}
                {...(editor.message
                    ? { error: t(`accountAction.nicknameMessages.${editor.message}`) }
                    : {})}
            />
            <Button
                variant="ghost"
                type="submit"
                className="settings-edit-button"
                loading={editor.busy}
                aria-label={t("accountAction.save")}
            >
                <Icon name="check" variant="outline" />
            </Button>
            <Button
                variant="ghost"
                className="settings-edit-button"
                onClick={close}
                disabled={editor.busy}
                aria-label={t("common.cancel")}
            >
                <Icon name="close" variant="outline" />
            </Button>
        </form>
    ) : (
        <>
            <span>{nickname}</span>
            <Button
                ref={trigger}
                variant="ghost"
                className="settings-edit-button"
                aria-label={t("accountAction.editNickname")}
                onClick={() => {
                    setDraft(nickname);
                    editor.reset();
                    setEditing(true);
                }}
            >
                <Icon name="edit" />
            </Button>
        </>
    );
}
