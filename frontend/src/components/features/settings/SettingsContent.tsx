import { calendarToday, formatCalendarDate } from "@/components/ui/inputs/calendar";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import {
    Avatar,
    AvatarPicker,
    IconChoiceGroup,
    ThemePicker,
    DatePicker,
    FilePicker,
    ImageCropDialog,
    Divider,
    avatarPresets,
    Button,
    ButtonLink,
    DropdownMenu,
    Icon,
    KeyboardShortcut,
    TextField,
    Tooltip,
} from "@/components/ui";
import type { ProfilePhoto } from "@/hooks/account.types";
import type { AuthState } from "@/hooks/auth.types";
import type { ThemePreference } from "@/hooks/useTheme";
import { formatDate, normalizeLanguage, type Language } from "@/i18n/locale";
import { pagePaths } from "@/routing/paths";
import type {
    AccountPurpose,
    LoginMethods,
    NicknameEditor,
    ProfileEditor,
    ProfileDetailsUpdate,
} from "@/hooks/account.types";

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
    className?: string;
};

export function SettingsRow({
    label,
    description,
    to,
    children,
    danger = false,
    className = "",
}: SettingsRowProps) {
    const rowClassName = `settings-row ${danger ? "settings-row--danger" : ""} ${className}`;
    const content = (
        <>
            <span className="settings-row__copy">
                <strong>{label}</strong>
                {description && <span>{description}</span>}
            </span>
            <div className="settings-row__value">
                {children}
                {to && <Icon name="chevron" variant="outline" />}
            </div>
            <Divider className="settings-row__divider" />
        </>
    );
    return to ? (
        <ButtonLink to={to} variant="plain" className={`${rowClassName} settings-row--link`}>
            {content}
        </ButtonLink>
    ) : (
        <div className={rowClassName}>{content}</div>
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
    const { t, i18n } = useTranslation();
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
                <SettingsRow label={t("settings.joinedAt")}>
                    <span>{formatDate(i18n.resolvedLanguage ?? "ko", member.createdAt)}</span>
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
    imageEditor,
    detailsEditor,
}: {
    member: Member;
    nicknameEditor: NicknameEditor;
    imageEditor: ProfilePhoto;
    detailsEditor: ProfileEditor<ProfileDetailsUpdate>;
}) {
    const { t, i18n } = useTranslation();
    const { previewUrl, changed } = imageEditor;
    const [gender, setGender] = useState(member.gender);
    const [birthDate, setBirthDate] = useState(member.birthDate ?? "");
    const detailsChanged = gender !== member.gender || birthDate !== (member.birthDate ?? "");
    return (
        <div className="settings-details">
            <div className="settings-profile settings-profile--hero">
                <FilePicker
                    label={t("settings.uploadPhoto")}
                    accept="image/jpeg,image/png"
                    disabled={imageEditor.busy}
                    className="settings-profile__photo"
                    onSelect={(file) => {
                        void imageEditor.selectFile(file);
                    }}
                >
                    <Avatar src={previewUrl} size="large" />
                    <span className="settings-profile__photo-edit" aria-hidden="true">
                        <Icon name="edit" />
                    </span>
                </FilePicker>
                <div>
                    <span className="settings-profile__eyebrow">{t("settings.yourJourney")}</span>
                    <InlineNickname
                        nickname={member.nickname}
                        editor={nicknameEditor}
                        appearance="hero"
                    />
                    <p>{t("settings.profileDescription")}</p>
                </div>
            </div>
            <div className="settings-photo-upload">
                <FilePicker
                    label={t("settings.uploadPhoto")}
                    accept="image/jpeg,image/png"
                    disabled={imageEditor.busy}
                    onSelect={(file) => {
                        void imageEditor.selectFile(file);
                    }}
                />
                <span>{t("settings.photoFileHint")}</span>
            </div>
            {imageEditor.cropSource && (
                <ImageCropDialog
                    key={imageEditor.cropSource}
                    source={imageEditor.cropSource}
                    title={t("settings.cropPhoto")}
                    hint={t("settings.cropPhotoHint")}
                    applyLabel={t("settings.applyPhoto")}
                    cancelLabel={t("common.cancel")}
                    loadError={t("settings.imageMessages.fileInvalid")}
                    {...(imageEditor.message
                        ? { error: t(`settings.imageMessages.${imageEditor.message}`) }
                        : {})}
                    busy={imageEditor.busy}
                    onClose={imageEditor.closeCrop}
                    onApply={(dataUrl) => {
                        void imageEditor.applyPhoto(dataUrl);
                    }}
                />
            )}
            <section className="settings-avatar-picker" aria-labelledby="avatar-picker-title">
                <h2 className="settings-section-title" id="avatar-picker-title">
                    {t("settings.chooseAvatar")}
                </h2>
                <AvatarPicker
                    label={t("settings.chooseAvatar")}
                    value={
                        avatarPresets.find(
                            (preset) => previewUrl === `/brand/avatars/${preset}.svg`,
                        ) ?? (!previewUrl ? "coast" : null)
                    }
                    options={avatarPresets.map((preset) => ({
                        value: preset,
                        label: t(`mockup.avatars.${preset}`),
                    }))}
                    disabled={imageEditor.busy}
                    onChange={(preset) => imageEditor.selectPreset(`/brand/avatars/${preset}.svg`)}
                />
                {imageEditor.message && (
                    <p className="auth-feedback" role="alert">
                        {t(`settings.imageMessages.${imageEditor.message}`)}
                    </p>
                )}
                <div className="settings-avatar-picker__actions">
                    <Button
                        variant="outline"
                        disabled={!changed || imageEditor.busy}
                        onClick={imageEditor.cancel}
                    >
                        {t("common.cancel")}
                    </Button>
                    <Button
                        variant="brand"
                        disabled={!changed || imageEditor.busy}
                        loading={imageEditor.busy}
                        onClick={() => {
                            void imageEditor.save();
                        }}
                    >
                        {t("accountAction.save")}
                    </Button>
                </div>
            </section>
            <Divider />
            <section
                className="settings-profile-information"
                aria-labelledby="profile-information-title"
            >
                <h2 className="settings-section-title" id="profile-information-title">
                    {t("settings.basicInformation")}
                </h2>
                <SettingsRow label={t("settings.nickname")}>
                    <InlineNickname nickname={member.nickname} editor={nicknameEditor} />
                </SettingsRow>
                <form
                    className="settings-profile-form"
                    onSubmit={(event) => {
                        event.preventDefault();
                        if (!detailsChanged || detailsEditor.busy) return;
                        const input: ProfileDetailsUpdate = {};
                        if (gender !== member.gender) input.gender = gender;
                        if (birthDate !== (member.birthDate ?? ""))
                            input.birthDate = birthDate || null;
                        void detailsEditor.save(input);
                    }}
                >
                    <SettingsRow label={t("settings.gender")}>
                        <IconChoiceGroup
                            label={t("settings.gender")}
                            value={gender ?? "unset"}
                            options={[
                                {
                                    value: "unset",
                                    label: t("settings.genderUnset"),
                                    icon: "gender-unset",
                                },
                                {
                                    value: "MALE",
                                    label: t("settings.genderValues.MALE"),
                                    icon: "gender-male",
                                },
                                {
                                    value: "FEMALE",
                                    label: t("settings.genderValues.FEMALE"),
                                    icon: "gender-female",
                                },
                            ]}
                            disabled={detailsEditor.busy}
                            onChange={(value) => setGender(value === "unset" ? null : value)}
                        />
                    </SettingsRow>
                    <SettingsRow label={t("settings.birthday")}>
                        <DatePicker
                            className="ui-date-picker--inline"
                            label={t("settings.birthday")}
                            yearNavigation
                            name="birthDate"
                            value={birthDate}
                            min={`${calendarToday().getUTCFullYear() - 100}-01-01`}
                            max={formatCalendarDate(calendarToday())}
                            disabled={detailsEditor.busy}
                            onChange={setBirthDate}
                        />
                    </SettingsRow>
                    <SettingsRow label={t("settings.joinedAt")}>
                        <span>{formatDate(i18n.resolvedLanguage ?? "ko", member.createdAt)}</span>
                    </SettingsRow>
                    {detailsEditor.message && (
                        <p className="auth-feedback" role="alert">
                            {t(`settings.detailsMessages.${detailsEditor.message}`)}
                        </p>
                    )}
                    <div className="settings-avatar-picker__actions">
                        <Button
                            variant="outline"
                            disabled={!detailsChanged || detailsEditor.busy}
                            onClick={() => {
                                setGender(member.gender);
                                setBirthDate(member.birthDate ?? "");
                            }}
                        >
                            {t("common.cancel")}
                        </Button>
                        <Button
                            variant="brand"
                            type="submit"
                            loading={detailsEditor.busy}
                            disabled={!detailsChanged || detailsEditor.busy}
                        >
                            {t("accountAction.save")}
                        </Button>
                    </div>
                </form>
            </section>
            <Divider className="settings-section-divider" />
            <ButtonLink to={pagePaths.account} variant="plain" className="profile-account-link">
                <Icon name="settings" />
                {t("settings.goToAccount")}
                <Icon name="chevron" variant="outline" />
            </ButtonLink>
        </div>
    );
}

type AppearanceSettingsProps = {
    theme: ThemePreference;
    onThemeChange: (preference: ThemePreference) => void;
};

export function AppearanceSettings({ theme, onThemeChange }: AppearanceSettingsProps) {
    const { t } = useTranslation();
    return (
        <div className="settings-details">
            <SettingsRow label={t("settings.themeSettings")} className="settings-row--theme">
                <ThemePicker
                    label={t("settings.themeSettings")}
                    value={theme}
                    onChange={onThemeChange}
                    options={(["system", "light", "dark"] as const).map((value) => ({
                        value,
                        label: t(`profile.${value}`),
                    }))}
                />
            </SettingsRow>
        </div>
    );
}

export function LanguageSettings({
    onLanguageChange,
}: {
    onLanguageChange: (language: Language) => void;
}) {
    const { t, i18n } = useTranslation();
    return (
        <div className="settings-details">
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

export function KeyboardSettings() {
    const { t } = useTranslation();
    return (
        <div className="settings-details">
            <SettingsRow label={t("settings.openSettings")}>
                <KeyboardShortcut keys={["mod", ","]} />
            </SettingsRow>
            <SettingsRow label={t("settings.openProfile")}>
                <KeyboardShortcut keys={["shift", "mod", "P"]} />
            </SettingsRow>
        </div>
    );
}

function InlineNickname({
    nickname,
    editor,
    appearance = "row",
}: {
    nickname: string;
    editor: NicknameEditor;
    appearance?: "row" | "hero";
}) {
    const { t } = useTranslation();
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(nickname);
    const trigger = useRef<HTMLButtonElement>(null);
    const form = useRef<HTMLFormElement>(null);
    useEffect(() => {
        if (!editing || editor.busy) return;
        const dismiss = (event: PointerEvent): void => {
            if (event.target instanceof Node && !form.current?.contains(event.target)) {
                setEditing(false);
                setDraft(nickname);
            }
        };
        document.addEventListener("pointerdown", dismiss);
        return () => document.removeEventListener("pointerdown", dismiss);
    }, [editing, editor.busy, nickname]);
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
            ref={form}
            className={`settings-nickname-editor${appearance === "hero" ? " settings-nickname-editor--hero" : ""}`}
            onBlur={(event) => {
                if (!editor.busy && !event.currentTarget.contains(event.relatedTarget)) {
                    setEditing(false);
                    setDraft(nickname);
                }
            }}
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
                disabled={editor.busy || draft.trim() === nickname}
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
    ) : appearance === "hero" ? (
        <Button
            ref={trigger}
            variant="plain"
            className="settings-profile__nickname"
            aria-label={`${t("accountAction.editNickname")}, ${nickname}`}
            onClick={() => {
                setDraft(nickname);
                editor.reset();
                setEditing(true);
            }}
        >
            <strong>{nickname}</strong>
            <Icon name="edit" />
        </Button>
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
