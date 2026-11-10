import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { Button } from "../buttons/Button";
import { Modal } from "../overlays/Modal";

type ImageCropDialogProps = {
    source: string;
    title: string;
    hint: string;
    applyLabel: string;
    cancelLabel: string;
    loadError: string;
    error?: string;
    busy: boolean;
    onApply: (dataUrl: string) => void;
    onClose: () => void;
};
type Point = { x: number; y: number };
const SIZE = 512;

function geometry(image: HTMLImageElement, zoom: number, point: Point) {
    const scale = SIZE / Math.max(image.naturalWidth, image.naturalHeight);
    const width = image.naturalWidth * scale;
    const height = image.naturalHeight * scale;
    const left = (SIZE - width) / 2;
    const top = (SIZE - height) / 2;
    const diameter = (Math.min(width, height) * 0.9) / zoom;
    const radius = diameter / 2;
    return {
        scale,
        width,
        height,
        left,
        top,
        radius,
        diameter,
        x: Math.max(left + radius, Math.min(left + width - radius, point.x)),
        y: Math.max(top + radius, Math.min(top + height - radius, point.y)),
    };
}

/** 원본 위의 원형 선택 영역을 이동하고, 선택한 부분만 JPEG로 반환한다. */
export function ImageCropDialog({
    source,
    title,
    hint,
    applyLabel,
    cancelLabel,
    loadError,
    error,
    busy,
    onApply,
    onClose,
}: ImageCropDialogProps) {
    const id = useId();
    const canvas = useRef<HTMLCanvasElement>(null);
    const [image, setImage] = useState<HTMLImageElement | null>(null);
    const [failed, setFailed] = useState(false);
    const [zoom, setZoom] = useState(1);
    const [point, setPoint] = useState<Point>({ x: SIZE / 2, y: SIZE / 2 });
    const drag = useRef<{ mode: "move" | "resize"; offset: Point; center: Point } | null>(null);
    const [adjusting, setAdjusting] = useState(false);
    useEffect(() => {
        let current = true;
        const next = new Image();
        next.onload = () => {
            if (current) setImage(next);
        };
        next.onerror = () => {
            if (current) setFailed(true);
        };
        next.src = source;
        return () => {
            current = false;
        };
    }, [source]);
    useLayoutEffect(() => {
        const context = canvas.current?.getContext("2d");
        if (!image || !context) return;
        const g = geometry(image, zoom, point);
        context.clearRect(0, 0, SIZE, SIZE);
        context.drawImage(image, g.left, g.top, g.width, g.height);
        context.fillStyle = "rgba(10, 20, 36, 0.52)";
        context.fillRect(g.left, g.top, g.width, g.height);
        context.save();
        context.beginPath();
        context.arc(g.x, g.y, g.radius, 0, 2 * Math.PI);
        context.clip();
        context.drawImage(image, g.left, g.top, g.width, g.height);
        if (adjusting) {
            context.strokeStyle = "rgba(255, 255, 255, 0.5)";
            context.lineWidth = 1.5;
            for (const fraction of [1 / 3, 2 / 3]) {
                const position = g.diameter * fraction;
                context.beginPath();
                context.moveTo(g.x - g.radius + position, g.y - g.radius);
                context.lineTo(g.x - g.radius + position, g.y + g.radius);
                context.moveTo(g.x - g.radius, g.y - g.radius + position);
                context.lineTo(g.x + g.radius, g.y - g.radius + position);
                context.stroke();
            }
        }
        context.restore();
        context.beginPath();
        context.arc(g.x, g.y, g.radius, 0, 2 * Math.PI);
        context.strokeStyle = "#fff";
        context.lineWidth = 3;
        context.stroke();
        context.strokeStyle = "rgba(255, 255, 255, 0.55)";
        context.lineWidth = 1.5;
        context.strokeRect(g.x - g.radius, g.y - g.radius, g.diameter, g.diameter);
        for (const x of [g.x - g.radius, g.x + g.radius]) {
            for (const y of [g.y - g.radius, g.y + g.radius]) {
                context.fillStyle = "#fff";
                context.fillRect(x - 6, y - 6, 12, 12);
                context.strokeStyle = "#385ba7";
                context.strokeRect(x - 6, y - 6, 12, 12);
            }
        }
    }, [image, zoom, point, adjusting]);

    function apply(): void {
        if (!image || busy) return;
        const g = geometry(image, zoom, point);
        const output = document.createElement("canvas");
        output.width = output.height = SIZE;
        const context = output.getContext("2d");
        if (!context) return;
        context.fillStyle = "#fff";
        context.fillRect(0, 0, SIZE, SIZE);
        context.drawImage(
            image,
            (g.x - g.radius - g.left) / g.scale,
            (g.y - g.radius - g.top) / g.scale,
            g.diameter / g.scale,
            g.diameter / g.scale,
            0,
            0,
            SIZE,
            SIZE,
        );
        onApply(output.toDataURL("image/jpeg", 0.9));
    }

    const feedback = failed ? loadError : error;
    return (
        <Modal
            open
            onClose={onClose}
            labelledBy={id}
            className="ui-image-crop"
            dismissible={!busy}
            closeOnBackdrop={false}
        >
            <h2 id={id}>{title}</h2>
            <p id={`${id}-hint`} className="ui-image-crop__hint">
                {hint}
            </p>
            <div className="ui-image-crop__stage">
                <canvas
                    ref={canvas}
                    width={SIZE}
                    height={SIZE}
                    tabIndex={0}
                    aria-label={title}
                    aria-describedby={`${id}-hint`}
                    onPointerDown={(event) => {
                        if (!image || busy) return;
                        event.currentTarget.setPointerCapture(event.pointerId);
                        const rect = event.currentTarget.getBoundingClientRect();
                        const x = ((event.clientX - rect.left) * SIZE) / rect.width;
                        const y = ((event.clientY - rect.top) * SIZE) / rect.height;
                        const g = geometry(image, zoom, point);
                        const corner =
                            Math.abs(Math.abs(x - g.x) - g.radius) < 22 &&
                            Math.abs(Math.abs(y - g.y) - g.radius) < 22;
                        const inside = Math.hypot(x - g.x, y - g.y) <= g.radius;
                        drag.current = {
                            mode: corner ? "resize" : "move",
                            center: { x: g.x, y: g.y },
                            offset: inside ? { x: x - g.x, y: y - g.y } : { x: 0, y: 0 },
                        };
                        setAdjusting(true);
                        if (!corner && !inside) {
                            const next = geometry(image, zoom, { x, y });
                            setPoint({ x: next.x, y: next.y });
                        }
                    }}
                    onPointerMove={(event) => {
                        if (!image || !drag.current || busy) return;
                        const rect = event.currentTarget.getBoundingClientRect();
                        const x = ((event.clientX - rect.left) * SIZE) / rect.width;
                        const y = ((event.clientY - rect.top) * SIZE) / rect.height;
                        if (drag.current.mode === "resize") {
                            const g = geometry(image, zoom, point);
                            const minimum = Math.min(g.width, g.height);
                            const diameter = Math.max(
                                minimum * 0.2,
                                Math.min(
                                    minimum,
                                    Math.max(
                                        Math.abs(x - drag.current.center.x),
                                        Math.abs(y - drag.current.center.y),
                                    ) * 2,
                                ),
                            );
                            const nextZoom = (minimum * 0.9) / diameter;
                            setZoom(nextZoom);
                            const next = geometry(image, nextZoom, drag.current.center);
                            setPoint({ x: next.x, y: next.y });
                        } else {
                            const next = geometry(image, zoom, {
                                x: x - drag.current.offset.x,
                                y: y - drag.current.offset.y,
                            });
                            setPoint({ x: next.x, y: next.y });
                        }
                    }}
                    onPointerUp={() => {
                        drag.current = null;
                        setAdjusting(false);
                    }}
                    onPointerCancel={() => {
                        drag.current = null;
                        setAdjusting(false);
                    }}
                    onKeyDown={(event) => {
                        if (!image || busy) return;
                        if (["+", "=", "-"].includes(event.key)) {
                            event.preventDefault();
                            setZoom(
                                Math.max(
                                    0.9,
                                    Math.min(4.5, zoom * (event.key === "-" ? 0.92 : 1.08)),
                                ),
                            );
                            return;
                        }
                        const delta = {
                            ArrowLeft: [-12, 0],
                            ArrowRight: [12, 0],
                            ArrowUp: [0, -12],
                            ArrowDown: [0, 12],
                        }[event.key];
                        if (!delta) return;
                        event.preventDefault();
                        const next = geometry(image, zoom, {
                            x: point.x + (delta[0] ?? 0),
                            y: point.y + (delta[1] ?? 0),
                        });
                        setPoint({ x: next.x, y: next.y });
                    }}
                />
            </div>
            {feedback && (
                <p className="ui-image-crop__error" role="alert">
                    {feedback}
                </p>
            )}
            <div className="ui-image-crop__actions">
                <Button variant="ghost" disabled={busy} onClick={onClose}>
                    {cancelLabel}
                </Button>
                <Button
                    variant="brand"
                    disabled={!image || failed || busy}
                    loading={busy}
                    onClick={apply}
                >
                    {applyLabel}
                </Button>
            </div>
        </Modal>
    );
}
