import { useEffect, useRef, useState } from "react";
import { useProfileEditor } from "./useProfileEditor";

import type { ProfilePhoto } from "../../account.types";

type Selection = { kind: "preset" | "upload"; url: string };

export function useProfilePhoto(
    savedUrl: string | null,
    updatePreset: (url: string, signal: AbortSignal) => Promise<void>,
    upload: (dataUrl: string, signal: AbortSignal) => Promise<void>,
): ProfilePhoto {
    const [selection, setSelection] = useState<Selection | null>(null);
    const [processing, setProcessing] = useState(false);
    const [cropSource, setCropSource] = useState<string | null>(null);
    useEffect(
        () => () => {
            if (cropSource) URL.revokeObjectURL(cropSource);
        },
        [cropSource],
    );
    const [fileMessage, setFileMessage] = useState<"fileInvalid" | "fileTooLarge" | null>(null);
    const revision = useRef(0);
    useEffect(
        () => () => {
            revision.current++;
        },
        [],
    );
    const editor = useProfileEditor<Selection>((input, signal) =>
        input.kind === "upload" ? upload(input.url, signal) : updatePreset(input.url, signal),
    );
    const busy = processing || editor.busy;
    const changed =
        selection !== null && selection.url !== (savedUrl ?? "/brand/avatars/coast.svg");

    async function selectFile(file: File): Promise<void> {
        if (busy) return;
        setFileMessage(null);
        editor.reset();
        if (!["image/jpeg", "image/png"].includes(file.type)) {
            setFileMessage("fileInvalid");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setFileMessage("fileTooLarge");
            return;
        }
        const current = ++revision.current;
        setProcessing(true);
        try {
            const image = await createImageBitmap(file);
            try {
                if (
                    image.width < 1 ||
                    image.height < 1 ||
                    image.width > 8192 ||
                    image.height > 8192
                )
                    throw new Error("image dimensions");
                if (current === revision.current) setCropSource(URL.createObjectURL(file));
            } finally {
                image.close();
            }
        } catch {
            if (current === revision.current) setFileMessage("fileInvalid");
        } finally {
            if (current === revision.current) setProcessing(false);
        }
    }

    return {
        previewUrl: selection?.url ?? savedUrl,
        cropSource,
        closeCrop(): void {
            if (!busy) {
                setCropSource(null);
                editor.reset();
            }
        },
        async applyPhoto(dataUrl: string): Promise<void> {
            if (busy) return;
            if (await editor.save({ kind: "upload", url: dataUrl })) {
                setSelection(null);
                setCropSource(null);
            }
        },
        changed,
        busy,
        message: fileMessage ?? editor.message,
        selectFile,
        selectPreset(url: string): void {
            if (busy) return;
            setFileMessage(null);
            editor.reset();
            setSelection({ kind: "preset", url });
        },
        cancel(): void {
            if (busy) return;
            setFileMessage(null);
            editor.reset();
            setSelection(null);
        },
        async save(): Promise<void> {
            if (!selection || !changed || busy) return;
            if (await editor.save(selection)) setSelection(null);
        },
    };
}
