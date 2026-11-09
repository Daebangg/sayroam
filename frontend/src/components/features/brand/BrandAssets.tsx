import { useTranslation } from "react-i18next";
import { Card } from "@/components/ui";

const layouts = ["logo", "mark", "wordmark"] as const;
const backgrounds = ["light", "dark"] as const;
const formats = ["svg", "png"] as const;

export function BrandAssets() {
    const { t } = useTranslation();
    return (
        <div className="brand-asset-library">
            {layouts.map((layout) => (
                <Card key={layout} className="brand-asset-card">
                    <h3>{t(`brand.assets.layouts.${layout}`)}</h3>
                    {backgrounds.map((background) => (
                        <figure className="brand-asset-variant" key={background}>
                            <div
                                className={`brand-asset-preview brand-asset-preview--${background}`}
                            >
                                <img
                                    className={`brand-asset-preview__image brand-asset-preview__image--${layout}`}
                                    src={`/brand/sayroam-${layout}-on-${background}.svg`}
                                    alt={t("brand.assets.preview", {
                                        layout: t(`brand.assets.layouts.${layout}`),
                                        background: t(`brand.assets.backgrounds.${background}`),
                                    })}
                                    loading="lazy"
                                />
                            </div>
                            <figcaption>
                                <span>{t(`brand.assets.backgrounds.${background}`)}</span>
                                <span className="brand-asset-links">
                                    {formats.map((format) => (
                                        <a
                                            key={format}
                                            href={`/brand/sayroam-${layout}-on-${background}.${format}`}
                                            download
                                            aria-label={t("brand.assets.download", {
                                                layout: t(`brand.assets.layouts.${layout}`),
                                                variant: t(
                                                    `brand.assets.backgrounds.${background}`,
                                                ),
                                                format: t(`brand.assets.formats.${format}`),
                                            })}
                                        >
                                            {t(`brand.assets.formats.${format}`)}
                                        </a>
                                    ))}
                                </span>
                            </figcaption>
                        </figure>
                    ))}
                    <div className="brand-asset-transparent">
                        {backgrounds.map((background) => (
                            <div key={background}>
                                <span>{t(`brand.assets.transparent.${background}`)}</span>
                                <span className="brand-asset-links">
                                    {formats.map((format) => (
                                        <a
                                            key={format}
                                            href={`/brand/sayroam-${layout}${background === "dark" ? "-light" : ""}.${format}`}
                                            download
                                            aria-label={t("brand.assets.download", {
                                                layout: t(`brand.assets.layouts.${layout}`),
                                                variant: t(
                                                    `brand.assets.transparent.${background}`,
                                                ),
                                                format: t(`brand.assets.formats.${format}`),
                                            })}
                                        >
                                            {t(`brand.assets.formats.${format}`)}
                                        </a>
                                    ))}
                                </span>
                            </div>
                        ))}
                    </div>
                </Card>
            ))}
        </div>
    );
}
