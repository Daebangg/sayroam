import { useOutletContext } from "react-router";
import type { PageContext } from "@/routing/context";
import { useState } from "react";
import { formatDateRange, formatNumber } from "@/i18n/locale";
import { useTranslation } from "react-i18next";
import {
    Brand,
    Button,
    ButtonLink,
    PageState,
    type PageStateKind,
    Card,
    Checkbox,
    DropdownMenu,
    Icon,
    KeyboardShortcut,
    Modal,
    Tag,
    TextField,
    Tooltip,
    type IconName,
} from "@/components/ui";

const colors = ["primary", "sky", "sand", "surface", "text", "muted"] as const;
const icons: IconName[] = ["home", "trips", "matching", "rooms", "bookmark", "profile"];

export function BrandPage() {
    const { onOpenAuth, onLanguageChange } = useOutletContext<PageContext>();
    const [pageState, setPageState] = useState<PageStateKind>("notFound");
    const { t, i18n } = useTranslation();
    const [selection, setSelection] = useState("planning");
    const [dialogOpen, setDialogOpen] = useState(false);
    const [pressed, setPressed] = useState(false);
    const [tagVisible, setTagVisible] = useState(true);
    const options = [
        { value: "planning", label: t("brand.planning") },
        { value: "looking", label: t("brand.looking") },
        { value: "ready", label: t("brand.ready") },
        { value: "unavailable", label: t("brand.unavailable"), disabled: true },
    ];

    return (
        <main id="main-content" tabIndex={-1} className="brand-page">
            <div className="page-heading">
                <h1>{t("brand.heading")}</h1>
                <Button variant="outline" onClick={onLanguageChange}>
                    {t("common.changeLanguage")}
                </Button>
            </div>
            <section className="brand-section" aria-labelledby="brand-page-states-heading">
                <h2 id="brand-page-states-heading" className="section-label">
                    {t("brand.pageStates")}
                </h2>
                <div
                    className="page-state-controls"
                    role="group"
                    aria-label={t("brand.pageStates")}
                >
                    {(["notFound", "error", "loading"] as const).map((kind) => (
                        <Button
                            key={kind}
                            variant="outline"
                            aria-pressed={pageState === kind}
                            onClick={() => setPageState(kind)}
                        >
                            {t(`pageState.${kind}.label`)}
                        </Button>
                    ))}
                    <ButtonLink to="/page-that-does-not-exist" variant="outline">
                        {t("brand.open404")}
                    </ButtonLink>
                </div>
                <Card className="page-state-preview">
                    <PageState
                        kind={pageState}
                        headingAs="h3"
                        title={t(`pageState.${pageState}.title`)}
                        description={t(`pageState.${pageState}.description`)}
                        actions={
                            pageState !== "loading" ? (
                                <>
                                    <ButtonLink to="/">{t("pageState.home")}</ButtonLink>
                                    {pageState === "error" && (
                                        <Button
                                            variant="outline"
                                            onClick={() => setPageState("loading")}
                                        >
                                            {t("pageState.retry")}
                                        </Button>
                                    )}
                                </>
                            ) : undefined
                        }
                    />
                </Card>
            </section>
            <section className="brand-section" aria-labelledby="brand-logo-heading">
                <h2 id="brand-logo-heading" className="section-label">
                    {t("brand.logo")}
                </h2>
                <div className="brand-preview">
                    <Brand />
                    <img
                        className="brand-preview__tile"
                        src="/brand/sayroam-app-icon.svg"
                        alt={t("brand.appIcon")}
                    />
                    <span className="brand-preview__tile brand-preview__tile--white">
                        <img src="/brand/sayroam-mark.svg" alt={t("brand.mark")} />
                    </span>
                </div>
                <div className="brand-downloads">
                    <a href="/brand/sayroam-logo.svg" download>
                        {t("brand.downloadLogo")}
                        <Icon name="arrow" />
                    </a>
                    <a href="/brand/sayroam-app-icon.svg" download>
                        {t("brand.downloadIcon")}
                        <Icon name="arrow" />
                    </a>
                    <a href="/brand/sayroam-logo.png" download>
                        {t("brand.downloadLogoPng")}
                    </a>
                    <a href="/brand/sayroam-logo-light.png" download>
                        {t("brand.downloadLightLogoPng")}
                    </a>
                    <a href="/brand/sayroam-wordmark.png" download>
                        {t("brand.downloadWordmarkPng")}
                    </a>
                    <a href="/brand/sayroam-wordmark-light.png" download>
                        {t("brand.downloadLightWordmarkPng")}
                    </a>
                    <a href="/brand/sayroam-mark.png" download>
                        {t("brand.downloadMarkPng")}
                    </a>
                    <a href="/brand/sayroam-mark-light.png" download>
                        {t("brand.downloadLightMarkPng")}
                    </a>
                    <a href="/brand/sayroam-app-icon.png" download>
                        {t("brand.downloadIconPng")}
                    </a>
                    <a href="/brand/favicon.png" download>
                        {t("brand.downloadFaviconPng")}
                    </a>
                </div>
            </section>
            <section className="brand-section" aria-labelledby="brand-wordmark-heading">
                <h2 id="brand-wordmark-heading" className="section-label">
                    {t("brand.wordmarks")}
                </h2>
                <div className="wordmark-variations">
                    <span className="wordmark-variations__primary">
                        {t("brand.wordmarkPrimary")}
                    </span>
                    <span>{t("brand.wordmarkCamel")}</span>
                    <span>{t("brand.wordmarkLower")}</span>
                    <span>{t("brand.wordmarkDot")}</span>
                </div>
            </section>
            <section className="brand-section" aria-labelledby="brand-palette-heading">
                <h2 id="brand-palette-heading" className="section-label">
                    {t("brand.palette")}
                </h2>
                <div className="palette">
                    {colors.map((color) => (
                        <div className="palette__item" key={color}>
                            <span className={`palette__swatch palette__swatch--${color}`} />
                            <strong>{t(`brand.colors.${color}.name`)}</strong>
                            <span>{t(`brand.colors.${color}.hex`)}</span>
                        </div>
                    ))}
                </div>
            </section>
            <section className="brand-section" aria-labelledby="brand-icons-heading">
                <h2 id="brand-icons-heading" className="section-label">
                    {t("brand.icons")}
                </h2>
                <div className="icon-showcase">
                    {icons.map((icon) => (
                        <span
                            className={`icon-showcase__item ${icon === "home" ? "is-active" : ""}`}
                            key={icon}
                        >
                            <Icon name={icon} />
                        </span>
                    ))}
                </div>
            </section>
            <section className="brand-section" aria-labelledby="brand-components-heading">
                <h2 id="brand-components-heading" className="section-label">
                    {t("brand.components")}
                </h2>
                <p className="catalogue-help">{t("brand.statesHelp")}</p>
                <div className="button-showcase">
                    <Button onClick={onOpenAuth}>
                        {t("home.start")}
                        <Icon name="arrow" />
                    </Button>
                    <Button variant="outline" aria-label={t("common.bookmark")}>
                        <Icon name="bookmark" />
                    </Button>
                    <Button variant="soft">
                        <Icon name="matching" />
                        {t("common.join")}
                    </Button>
                    <Button variant="ghost" disabled>
                        {t("common.disabled")}
                    </Button>
                </div>

                <div className="component-catalogue">
                    <Card className="component-example">
                        <h3>{t("brand.buttons")}</h3>
                        <div className="component-example__row">
                            <Button variant="brand">{t("brand.primaryButton")}</Button>
                            <Button variant="outline">{t("brand.secondaryButton")}</Button>
                            <Button variant="soft">{t("common.join")}</Button>
                            <Button variant="danger">{t("brand.delete")}</Button>
                            <Button disabled>{t("common.disabled")}</Button>
                            <Button
                                variant="outline"
                                aria-pressed={pressed}
                                onClick={() => setPressed(!pressed)}
                            >
                                {t("brand.selected")}
                            </Button>
                            <Button variant="outline">{t("brand.longAction")}</Button>
                            <Button variant="brand" loading>
                                {t("brand.save")}
                            </Button>
                        </div>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.dropdowns")}</h3>
                        <div className="component-example__row">
                            <DropdownMenu
                                label={t("brand.tripStatus")}
                                value={selection}
                                options={options}
                                onChange={setSelection}
                            />
                            <DropdownMenu
                                label={t("brand.tripStatus")}
                                value="planning"
                                options={options}
                                onChange={() => undefined}
                                disabled
                            />
                        </div>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.tags")}</h3>
                        <div className="component-example__row">
                            <Tag label={t("brand.tagFree")} />
                            <Tag label={t("brand.tagWinter")} tone="primary" />
                            <Tag label={t("brand.tagTogether")} tone="success" />
                            {tagVisible ? (
                                <Tag
                                    label={t("brand.tagRemovable")}
                                    tone="primary"
                                    onRemove={() => setTagVisible(false)}
                                />
                            ) : (
                                <Button variant="ghost" onClick={() => setTagVisible(true)}>
                                    {t("brand.reset")}
                                </Button>
                            )}
                        </div>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.tooltips")}</h3>
                        <div className="component-example__row">
                            <Tooltip content={t("common.search")} shortcut={["mod", "K"]}>
                                <Button
                                    variant="outline"
                                    className="icon-button"
                                    aria-label={t("common.search")}
                                >
                                    <Icon name="search" />
                                </Button>
                            </Tooltip>
                            <Tooltip content={t("brand.selectTool")} shortcut={["V"]}>
                                <Button
                                    variant="soft"
                                    className="icon-button"
                                    aria-label={t("brand.selectTool")}
                                >
                                    <Icon name="home" />
                                </Button>
                            </Tooltip>
                            <Tooltip content={t("common.close")} shortcut={["Escape"]}>
                                <Button variant="outline">{t("brand.hoverFocus")}</Button>
                            </Tooltip>
                            <KeyboardShortcut keys={["mod", "shift", "K"]} />
                        </div>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.inputs")}</h3>
                        <div className="component-example__fields">
                            <TextField
                                label={t("auth.emailLabel")}
                                type="email"
                                placeholder={t("brand.emailPlaceholder")}
                                hint={t("brand.inputHint")}
                            />
                            <TextField
                                label={t("brand.errorInput")}
                                defaultValue="invalid-email"
                                error={t("brand.inputError")}
                            />
                            <TextField
                                label={t("brand.disabledInput")}
                                disabled
                                placeholder={t("brand.emailPlaceholder")}
                            />
                            <Checkbox label={t("auth.remember")} />
                            <Checkbox label={t("brand.selected")} defaultChecked />
                            <Checkbox label={t("common.disabled")} defaultChecked disabled />
                            <Checkbox label={t("brand.longLabel")} />
                        </div>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.cards")}</h3>
                        <Card className="ui-card--example">
                            <h3>{t("brand.cardTitle")}</h3>
                            <p>{t("brand.cardDescription")}</p>
                        </Card>
                    </Card>
                    <Card className="component-example ui-card--example">
                        <h3>{t("brand.localeHeading")}</h3>
                        <p>
                            {t("brand.localizedExample", {
                                date: formatDateRange(
                                    i18n.resolvedLanguage ?? "ko",
                                    "2026-02-10",
                                    "2026-02-13",
                                ),
                                number: formatNumber(i18n.resolvedLanguage ?? "ko", 1234567.89),
                            })}
                        </p>
                    </Card>
                    <Card className="component-example">
                        <h3>{t("brand.modals")}</h3>
                        <div className="component-example__row">
                            <Button variant="outline" onClick={() => setDialogOpen(true)}>
                                {t("brand.openModal")}
                            </Button>
                            <Button variant="brand" onClick={onOpenAuth}>
                                {t("brand.openAuth")}
                            </Button>
                        </div>
                    </Card>
                </div>
            </section>
            <Modal
                open={dialogOpen}
                onClose={() => setDialogOpen(false)}
                labelledBy="brand-modal-heading"
                className="ui-modal--showcase"
            >
                <Button
                    variant="ghost"
                    className="icon-button ui-modal__close"
                    onClick={() => setDialogOpen(false)}
                    aria-label={t("common.close")}
                >
                    <Icon name="close" />
                </Button>
                <h2 id="brand-modal-heading">{t("brand.modalTitle")}</h2>
                <p className="ui-modal__description">{t("brand.modalDescription")}</p>
                <div className="ui-modal__footer">
                    <Button variant="outline" onClick={() => setDialogOpen(false)}>
                        {t("brand.cancel")}
                    </Button>
                    <Button variant="brand" onClick={() => setDialogOpen(false)}>
                        {t("brand.continue")}
                    </Button>
                </div>
            </Modal>
        </main>
    );
}
