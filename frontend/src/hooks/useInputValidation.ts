import { useEffect, useEffectEvent } from "react";

export type InputValidationMessage = "required" | "email" | "url" | "invalid";

/** 모든 폼의 native invalid 팝업을 막고 첫 오류만 공통 피드백으로 전달한다. */
export function useInputValidation(
    onInvalid: (message: InputValidationMessage, field: string) => void,
): void {
    const notify = useEffectEvent(onInvalid);
    useEffect(() => {
        let reported = false;
        const invalid = (event: Event): void => {
            const input = event.target;
            if (!(
                input instanceof HTMLInputElement ||
                input instanceof HTMLTextAreaElement ||
                input instanceof HTMLSelectElement
            ))
                return;
            event.preventDefault();
            if (reported) return;
            reported = true;
            queueMicrotask(() => {
                reported = false;
            });
            const field =
                input.labels?.[0]?.textContent?.trim() || input.getAttribute("aria-label") || "";
            const message: InputValidationMessage = input.validity.valueMissing
                ? "required"
                : input instanceof HTMLInputElement &&
                    input.validity.typeMismatch &&
                    input.type === "email"
                  ? "email"
                  : input instanceof HTMLInputElement &&
                      input.validity.typeMismatch &&
                      input.type === "url"
                    ? "url"
                    : "invalid";
            notify(message, field);
            input.focus({ preventScroll: true });
        };
        document.addEventListener("invalid", invalid, true);
        return () => document.removeEventListener("invalid", invalid, true);
    }, []);
}
