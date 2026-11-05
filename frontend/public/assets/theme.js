(() => {
    const storageKey = "sayroam.theme.v1";
    const systemTheme = window.matchMedia("(prefers-color-scheme: dark)");
    const listeners = new Set();
    const normalize = (value) => (value === "light" || value === "dark" ? value : "system");
    const read = () => {
        try {
            return normalize(window.localStorage.getItem(storageKey));
        } catch {
            return "system";
        }
    };
    let snapshot;
    const apply = (preference) => {
        const resolved =
            preference === "system" ? (systemTheme.matches ? "dark" : "light") : preference;
        document.documentElement.dataset.theme = resolved;
        if (snapshot?.preference === preference && snapshot.resolved === resolved) return;
        snapshot = Object.freeze({ preference, resolved });
        listeners.forEach((listener) => listener());
    };

    // 실행 순서: 저장된 선택 → 미리보기 URL → 첫 화면과 React가 공유하는 상태.
    const preview = new URLSearchParams(window.location.search).get("theme");
    apply(preview === null ? read() : normalize(preview));
    window.sayroamTheme = Object.freeze({
        getSnapshot: () => snapshot,
        subscribe: (listener) => {
            listeners.add(listener);
            return () => listeners.delete(listener);
        },
        setPreference: (value) => {
            const preference = normalize(value);
            apply(preference);
            try {
                window.localStorage.setItem(storageKey, preference);
            } catch {
                // 저장이 제한된 브라우저에서도 현재 화면의 선택은 유지합니다.
            }
            const url = new URL(window.location.href);
            url.searchParams.delete("theme");
            window.history.replaceState(window.history.state, "", url);
        },
    });
    systemTheme.addEventListener("change", () => {
        if (snapshot.preference === "system") apply("system");
    });
    window.addEventListener("storage", (event) => {
        if (event.key === storageKey || event.key === null) apply(read());
    });
})();
