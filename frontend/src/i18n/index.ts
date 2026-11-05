import { createInstance } from "i18next";
import { initReactI18next } from "react-i18next";
import ko from "../locales/ko.json" with { type: "json" };
import en from "../locales/en.json" with { type: "json" };

const i18n = createInstance();

void i18n.use(initReactI18next).init({
    resources: {
        ko: { translation: ko },
        en: { translation: en satisfies typeof ko },
    },
    lng:
        typeof window !== "undefined" &&
        new URLSearchParams(window.location.search).get("lang") === "en"
            ? "en"
            : "ko",
    fallbackLng: "ko",
    supportedLngs: ["ko", "en"],
    defaultNS: "translation",
    initAsync: false,
    interpolation: {
        escapeValue: false,
    },
});

function updateDocumentLanguage(): void {
    if (typeof document === "undefined") return;

    document.documentElement.lang = i18n.resolvedLanguage ?? "ko";
    document.title = i18n.t("app.name");
    document
        .querySelector('meta[name="description"]')
        ?.setAttribute("content", i18n.t("app.description"));
}

i18n.on("languageChanged", updateDocumentLanguage);
updateDocumentLanguage();

export default i18n;
