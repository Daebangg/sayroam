import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, TextField } from "@/components/ui";

export type TitleSaveResult = "saved" | "tooLong" | "failed";

type TripTitleEditorProps = {
    title: string | null;
    maxLength: number;
    onSave: (title: string) => Promise<TitleSaveResult>;
};

export function TripTitleEditor({ title, maxLength, onSave }: TripTitleEditorProps) {
    const { t } = useTranslation();
    const [draft, setDraft] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    // 입력창이 사라질 때 생기는 blur가 저장·취소 뒤에 다시 저장하지 않도록 편집 중인지 따로 기억한다.
    const editing = useRef(false);

    function start() {
        editing.current = true;
        setDraft(title ?? "");
    }

    function finish() {
        editing.current = false;
        setDraft(null);
        setError(null);
    }

    async function save() {
        if (draft === null || !editing.current || saving) return;
        const next = draft.trim();
        if (next === (title ?? "")) {
            finish();
            return;
        }
        setSaving(true);
        const result = await onSave(next);
        setSaving(false);
        if (result === "saved") {
            finish();
        } else {
            setError(
                result === "tooLong"
                    ? t("tripDetail.titleTooLong", { max: maxLength })
                    : t("tripDetail.titleSaveFailed"),
            );
        }
    }

    if (draft === null)
        return (
            <div className="trip-title">
                <h1>{title ?? t("trips.untitled")}</h1>
                <Button
                    variant="plain"
                    className="trip-title__edit"
                    aria-label={t("tripDetail.editTitle")}
                    onClick={start}
                >
                    <Icon name="edit" variant="outline" />
                </Button>
            </div>
        );

    return (
        <form
            className="trip-title trip-title--editing"
            onSubmit={(event) => {
                event.preventDefault();
                void save();
            }}
        >
            <TextField
                label={t("tripDetail.titleLabel")}
                value={draft}
                autoFocus
                readOnly={saving}
                placeholder={t("trips.untitled")}
                {...(error ? { error } : {})}
                onFocus={(event) => event.currentTarget.select()}
                onChange={(event) => {
                    setDraft(event.currentTarget.value);
                    setError(null);
                }}
                onBlur={() => void save()}
                onKeyDown={(event) => {
                    if (event.key === "Escape") finish();
                }}
            />
        </form>
    );
}
