import { createInstance } from "i18next";
import { normalizeLanguage, type Language } from "./locale";
import { initReactI18next } from "react-i18next";
import ko from "../locales/ko.json" with { type: "json" };
import en from "../locales/en.json" with { type: "json" };

const languageStorageKey = "sayroam.language.v1";

function savedLanguage(): Language {
    try {
        return normalizeLanguage(window.localStorage.getItem(languageStorageKey));
    } catch {
        return "ko";
    }
}

function initialLanguage(): Language {
    if (typeof window === "undefined") return "ko";
    const preview = new URLSearchParams(window.location.search).get("lang");
    return preview === null ? savedLanguage() : normalizeLanguage(preview);
}

const i18n = createInstance();

void i18n.use(initReactI18next).init({
    resources: {
        ko: { translation: ko },
        en: { translation: en satisfies typeof ko },
    },
    lng: initialLanguage(),
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

export function setLanguage(language: Language): void {
    const supported = normalizeLanguage(language);
    void i18n.changeLanguage(supported);
    try {
        window.localStorage.setItem(languageStorageKey, supported);
    } catch {
        // 저장이 제한된 브라우저에서도 현재 화면의 선택은 유지합니다.
    }
    const url = new URL(window.location.href);
    url.searchParams.delete("lang");
    window.history.replaceState(window.history.state, "", url);
}

if (typeof window !== "undefined") {
    window.addEventListener("storage", (event) => {
        if (event.key === languageStorageKey || event.key === null) {
            void i18n.changeLanguage(savedLanguage());
        }
    });
}
