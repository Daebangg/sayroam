import { useEffect, useRef } from "react";

import { useTheme } from "@/hooks/useTheme";

type LivingCoastProps = {
    imageSrc?: string;
    className?: string;
    initialIntensity?: number;
    animated?: boolean;
};

// 밤하늘과 달빛은 정적인 레이어로 한 번 그려 프레임마다 풍경을 다시 채색하지 않는다.
function paintNightCoast(image: HTMLImageElement, width: number, height: number) {
    const backdrop = document.createElement("canvas");
    backdrop.width = width;
    backdrop.height = height;
    const ctx = backdrop.getContext("2d");
    if (!ctx) return image;
    ctx.drawImage(image, 0, 0, width, height);
    ctx.globalCompositeOperation = "multiply";
    const dusk = ctx.createLinearGradient(0, 0, 0, height);
    dusk.addColorStop(0, "#16294a");
    dusk.addColorStop(0.54, "#3e537c");
    dusk.addColorStop(0.76, "#687396");
    dusk.addColorStop(1, "#426795");
    ctx.fillStyle = dusk;
    ctx.fillRect(0, 0, width, height);
    ctx.globalCompositeOperation = "source-over";

    // 하늘의 빈 공간에만 별을 배치한다.
    for (const [x, y, radius] of [
        [195, 75, 1.3],
        [324, 178, 1.7],
        [447, 54, 1.1],
        [512, 230, 1.3],
        [651, 132, 2],
        [718, 184, 1.1],
        [795, 88, 1.4],
        [915, 216, 1.5],
        [1007, 115, 1.8],
        [1060, 252, 1.1],
        [1173, 80, 1.4],
        [1255, 192, 1.3],
        [1402, 119, 1.5],
        [1525, 235, 1.2],
    ] as const) {
        ctx.fillStyle = "#e3edffb3";
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fill();
    }
    ctx.translate(1225, 142);
    const halo = ctx.createRadialGradient(0, 0, 8, 0, 0, 74);
    halo.addColorStop(0, "#e6daaf26");
    halo.addColorStop(1, "#e6daaf00");
    ctx.fillStyle = halo;
    ctx.fillRect(-74, -74, 148, 148);
    ctx.beginPath();
    ctx.moveTo(4, -19);
    ctx.bezierCurveTo(-24, -18, -25, 23, 7, 22);
    ctx.quadraticCurveTo(17, 21, 22, 14);
    ctx.bezierCurveTo(-1, 19, -11, -5, 4, -19);
    ctx.fillStyle = "#ffebba";
    ctx.fill();
    return backdrop;
}

// Drawing routines are adapted from the supplied Living Coast v4 component.
export function LivingCoast({
    imageSrc = "/images/sayroam-coast.png",
    className = "",
    initialIntensity = 1.15,
    animated = true,
}: LivingCoastProps) {
    const { resolved } = useTheme();
    const night = resolved === "dark";
    const canvasRef = useRef<HTMLCanvasElement>(null);

    useEffect(() => {
        const element = canvasRef.current;
        const context = element?.getContext("2d");
        if (!element || !context) return;
        const canvas = element;
        const ctx = context;
        const W = 1672,
            H = 941;
        const img = new Image();
        let backdrop: CanvasImageSource = img;
        const intensity = initialIntensity;
        let t = 0;
        let last = 0;
        let frameId = 0;
        let disposed = false;
        let visible = true;
        const reducedMedia = window.matchMedia("(prefers-reduced-motion: reduce)");
        let reduced = reducedMedia.matches;

        function ready(): boolean {
            return img.complete && img.naturalWidth > 0;
        }
        function resize(): void {
            const rect = canvas.getBoundingClientRect();
            if (!rect.width || !rect.height) return;
            const dpr = Math.min(window.devicePixelRatio || 1, 2);
            canvas.width = Math.max(1, Math.round(rect.width * dpr));
            canvas.height = Math.max(1, Math.round(rect.height * dpr));
            // Cover the background without stretching the original scene.
            const scale = Math.max(canvas.width / W, canvas.height / H);
            ctx.setTransform(
                scale,
                0,
                0,
                scale,
                (canvas.width - W * scale) * 0.65,
                canvas.height - H * scale,
            );
            if (ready()) draw();
        }
        // 두 개의 부드러운 구름만 유지해 넓은 하늘 여백을 살립니다.
        const clouds = [
            { x: 430, y: 118, s: 1.16, a: 0.29, speed: 11 },
            { x: 1320, y: 194, s: 0.89, a: 0.33, speed: 14 },
        ];

        function drawCloudShape(scale = 1, alpha = 0.5) {
            ctx.save();
            ctx.scale(scale, scale);
            const grad = ctx.createLinearGradient(0, -44, 0, 24);
            grad.addColorStop(0, `rgba(255,255,255,${alpha * 0.95})`);
            grad.addColorStop(0.85, `rgba(246,251,255,${alpha * 0.78})`);
            grad.addColorStop(1, `rgba(237,246,255,${alpha * 0.58})`);
            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.moveTo(-110, 6);
            ctx.bezierCurveTo(-80, -2, -74, -25, -46, -23);
            ctx.bezierCurveTo(-28, -60, 20, -52, 29, -26);
            ctx.bezierCurveTo(47, -43, 87, -28, 88, -3);
            ctx.bezierCurveTo(113, 0, 122, 4, 137, 10);
            ctx.bezierCurveTo(80, 18, -45, 18, -110, 6);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }

        function drawCloudLayer() {
            for (const c of clouds) {
                const wrap = W + 420;
                let x = (c.x + t * c.speed * intensity) % wrap;
                x -= 210;
                const bob =
                    (Math.sin(t * 0.55 + c.x * 0.009) * 12 + Math.sin(t * 0.23 + c.x * 0.021) * 5) *
                    intensity;
                const sway = Math.sin(t * 0.36 + c.x * 0.017) * 9 * intensity;
                ctx.save();
                ctx.translate(x + sway, c.y + bob);
                drawCloudShape(c.s, night ? c.a * 0.32 : c.a);
                ctx.restore();
                if (x > W - 220) {
                    ctx.save();
                    ctx.translate(x - wrap + sway, c.y + bob);
                    drawCloudShape(c.s, night ? c.a * 0.32 : c.a);
                    ctx.restore();
                }
            }
        }

        function seaClip() {
            ctx.beginPath();
            ctx.moveTo(558, 716);
            ctx.lineTo(1310, 713);
            ctx.lineTo(1672, 715);
            ctx.lineTo(1672, 941);
            ctx.lineTo(705, 941);
            ctx.lineTo(638, 875);
            ctx.lineTo(612, 826);
            ctx.lineTo(571, 786);
            ctx.closePath();
            ctx.clip();
        }

        function waveBand(
            baseY: number,
            height: number,
            amp: number,
            freq1: number,
            freq2: number,
            speed1: number,
            speed2: number,
            phase: number,
            color1: string,
            color2: string,
            alpha: number,
        ) {
            ctx.beginPath();
            for (let x = 505; x <= 1735; x += 10) {
                const y =
                    baseY +
                    Math.sin(x * freq1 + t * speed1 + phase) * amp +
                    Math.sin(x * freq2 - t * speed2 + phase * 1.3) * amp * 0.42;
                if (x === 505) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            for (let x = 1735; x >= 505; x -= 10) {
                const y =
                    baseY +
                    height +
                    Math.sin(x * freq1 + t * speed1 + phase + 0.55) * amp +
                    Math.sin(x * freq2 - t * speed2 + phase * 1.3 + 0.4) * amp * 0.38;
                ctx.lineTo(x, y);
            }
            ctx.closePath();
            const g = ctx.createLinearGradient(0, baseY, 0, baseY + height);
            g.addColorStop(0, color1.replace("ALPHA", alpha.toFixed(3)));
            g.addColorStop(1, color2.replace("ALPHA", (alpha * 0.15).toFixed(3)));
            ctx.fillStyle = g;
            ctx.fill();
        }

        function glint(x: number, y: number, len: number, alpha: number) {
            ctx.save();
            ctx.translate(x, y);
            ctx.strokeStyle = `rgba(235,248,255,${alpha})`;
            ctx.lineWidth = 1.2;
            ctx.lineCap = "round";
            ctx.beginPath();
            ctx.moveTo(-len / 2, 0);
            ctx.quadraticCurveTo(0, -2.2, len / 2, 0);
            ctx.stroke();
            ctx.restore();
        }

        function drawSeaEffects() {
            ctx.save();
            seaClip();

            waveBand(
                754,
                22,
                8.8 * intensity,
                0.0075,
                0.018,
                0.82,
                0.47,
                0.2,
                "rgba(157,214,255,ALPHA)",
                "rgba(157,214,255,ALPHA)",
                0.24,
            );
            waveBand(
                812,
                34,
                12.0 * intensity,
                0.0086,
                0.021,
                0.62,
                0.54,
                1.1,
                "rgba(147,206,253,ALPHA)",
                "rgba(147,206,253,ALPHA)",
                0.29,
            );
            waveBand(
                868,
                44,
                15.2 * intensity,
                0.0095,
                0.024,
                0.54,
                0.46,
                2.4,
                "rgba(140,200,250,ALPHA)",
                "rgba(140,200,250,ALPHA)",
                0.31,
            );
            waveBand(
                913,
                46,
                17.0 * intensity,
                0.0102,
                0.028,
                0.46,
                0.34,
                3.4,
                "rgba(132,190,247,ALPHA)",
                "rgba(132,190,247,ALPHA)",
                0.22,
            );

            for (let i = 0; i < 6; i++) {
                const sx = 620 + ((i * 113) % 970);
                const sy = 742 + ((i * 59) % 180);
                const drift = Math.sin(t * 0.8 + i * 1.7) * 6 * intensity;
                const twinkle = 0.025 + (Math.sin(t * 0.9 + i * 2.1) + 1) * 0.5 * 0.08;
                glint(sx + drift, sy, 6 + (i % 3) * 3, twinkle);
            }

            // subtle large reflection sweep
            const sweep = ctx.createLinearGradient(
                720 + Math.sin(t * 0.22) * 90,
                760,
                1210 + Math.sin(t * 0.22) * 90,
                920,
            );
            sweep.addColorStop(0, "rgba(255,255,255,0)");
            sweep.addColorStop(0.45, "rgba(220,242,255,.05)");
            sweep.addColorStop(0.6, "rgba(255,255,255,.09)");
            sweep.addColorStop(1, "rgba(255,255,255,0)");
            ctx.fillStyle = sweep;
            ctx.beginPath();
            ctx.ellipse(1100, 860, 420, 72, -0.05, 0, Math.PI * 2);
            ctx.fill();

            ctx.restore();
        }

        const boatFacets = [
            // Broad, asymmetric sail planes echo the coastline's cubist shapes.
            ["#f9efd9", "M-4-61-36-1-4-8Z"],
            ["#a9cde9", "M-4-61-19-17-4-8Z"],
            ["#e7b977", "M-36-1-19-17-4-8Z"],
            ["#eaf4fc", "M3-55 30-5 3-9Z"],
            ["#78aede", "M3-55 14-27 3-9Z"],
            ["#c2dcef", "M14-27 30-5 3-9Z"],
            ["#174675", "M-2-63 1-64 2 5-1 5Z"],
            ["#e4ba7b", "M-10-3 12-4 17 2-14 4Z"],
            ["#1e4f83", "M-34 4 33 0 22 16-19 19-28 12Z"],
            ["#477bb6", "M-34 4-6 9-19 19-28 12Z"],
            ["#12355e", "M-6 9 33 0 22 16-19 19Z"],
            ["#85b8df", "M-34 4 33 0 5 6-6 9Z"],
        ] satisfies readonly (readonly [string, string])[];
        const boatPlanes = boatFacets.map(([color, path]) => ({ color, path: new Path2D(path) }));

        function drawBoat() {
            const bob = Math.sin(t * 1.35) * 3.2 * intensity;
            const roll = Math.sin(t * 1.05 + 0.8) * 0.018 * intensity;
            ctx.save();
            ctx.translate(1115, 758 + bob);
            ctx.rotate(roll);

            const reflection = ctx.createRadialGradient(0, 18, 0, 0, 18, 66);
            reflection.addColorStop(0, "rgba(218,241,255,.18)");
            reflection.addColorStop(1, "rgba(218,241,255,0)");
            ctx.fillStyle = reflection;
            ctx.beginPath();
            ctx.ellipse(0, 22, 64, 9, 0, 0, Math.PI * 2);
            ctx.fill();

            for (const facet of boatPlanes) {
                ctx.fillStyle = facet.color;
                ctx.fill(facet.path);
            }
            ctx.restore();
        }

        function drawSunlight() {
            // Warm diffuse daylight high in the sky, with very faint moving rays.
            ctx.save();
            ctx.globalCompositeOperation = "screen";
            const sx = 1230,
                sy = -140;
            const halo = ctx.createRadialGradient(sx, sy, 40, sx, sy, 1050);
            halo.addColorStop(0, "rgba(255,246,212,.30)");
            halo.addColorStop(0.33, "rgba(255,240,203,.13)");
            halo.addColorStop(1, "rgba(255,255,255,0)");
            ctx.fillStyle = halo;
            ctx.fillRect(0, 0, W, H);
            const rayPhase = Math.sin(t * 0.23) * 0.028;
            for (let k = 0; k < 5; k++) {
                const angle = 0.28 + k * 0.105 + rayPhase;
                const spread = 0.025 + (k % 2) * 0.012;
                ctx.beginPath();
                ctx.moveTo(sx, sy);
                ctx.lineTo(
                    sx + Math.sin(angle - spread) * 1400,
                    sy + Math.cos(angle - spread) * 1400,
                );
                ctx.lineTo(
                    sx + Math.sin(angle + spread) * 1400,
                    sy + Math.cos(angle + spread) * 1400,
                );
                ctx.closePath();
                ctx.fillStyle = `rgba(255,247,222,${0.02 + (Math.sin(t * 0.4 + k) * 0.5 + 0.5) * 0.013})`;
                ctx.fill();
            }
            ctx.restore();
        }

        function drawSunReflections() {
            ctx.save();
            seaClip();
            ctx.globalCompositeOperation = "screen";
            const focusX = 1230;
            // Broken sun-glitter path to keep the sea painterly instead of photorealistic.
            for (let i = 0; i < 8; i++) {
                const depth = ((i * 47) % 200) / 200;
                const yy = 731 + depth * 190;
                const spread = 25 + depth * 145;
                const xx = focusX + Math.sin(i * 17.23 + t * 0.55) * spread;
                const pulse = Math.pow((1 + Math.sin(t * (1.1 + (i % 5) * 0.13) + i * 3.9)) / 2, 2);
                glint(
                    xx,
                    yy + Math.sin(t * 0.85 + i) * 3 * intensity,
                    6 + (i % 3) * 3,
                    (0.02 + 0.09 * pulse) * (1 - depth * 0.35),
                );
            }
            ctx.restore();
        }

        function draw() {
            ctx.clearRect(0, 0, W, H);
            ctx.drawImage(backdrop, 0, 0, W, H);
            if (!night) drawSunlight();
            drawCloudLayer();
            ctx.save();
            if (night) ctx.globalAlpha = 0.32;
            drawSeaEffects();
            drawSunReflections();
            ctx.restore();
            drawBoat();
        }

        function stop(): void {
            if (frameId) window.cancelAnimationFrame(frameId);
            frameId = 0;
            last = 0;
        }
        function canAnimate(): boolean {
            return animated && !disposed && visible && !document.hidden && !reduced && ready();
        }
        function loop(ms: number): void {
            frameId = 0;
            if (!canAnimate()) return;
            if (!last) last = ms;
            t += Math.min((ms - last) / 1000, 0.04);
            last = ms;
            draw();
            frameId = window.requestAnimationFrame(loop);
        }
        function syncPlayback(): void {
            stop();
            if (disposed || !ready()) return;
            if (visible && !document.hidden) draw();
            if (canAnimate()) frameId = window.requestAnimationFrame(loop);
        }
        const observer = new ResizeObserver(resize);
        observer.observe(canvas);
        const intersectionObserver = new IntersectionObserver(([entry]) => {
            visible = entry?.isIntersecting ?? false;
            syncPlayback();
        });
        intersectionObserver.observe(canvas);
        const onReducedMotionChange = (event: MediaQueryListEvent): void => {
            reduced = event.matches;
            syncPlayback();
        };
        reducedMedia.addEventListener("change", onReducedMotionChange);
        document.addEventListener("visibilitychange", syncPlayback);
        img.onload = () => {
            if (disposed) return;
            if (night) backdrop = paintNightCoast(img, W, H);
            resize();
            syncPlayback();
        };
        img.src = imageSrc;
        resize();

        return () => {
            disposed = true;
            stop();
            observer.disconnect();
            intersectionObserver.disconnect();
            reducedMedia.removeEventListener("change", onReducedMotionChange);
            document.removeEventListener("visibilitychange", syncPlayback);
            img.onload = null;
        };
    }, [imageSrc, initialIntensity, animated, night]);

    return (
        <div className={`living-coast ${className}`} aria-hidden="true">
            <canvas key={resolved} ref={canvasRef} className="living-coast__canvas" />
        </div>
    );
}
