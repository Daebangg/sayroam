import { useId } from "react";
import { discoveryGlyphs, discoveryWordmarks } from "./discoveryWordmarks";

type DiscoveryWordmarkProps = {
    state: keyof typeof discoveryWordmarks;
};

export function DiscoveryWordmark({ state }: DiscoveryWordmarkProps) {
    const clipId = useId();
    const wordmark = discoveryWordmarks[state];

    return (
        <svg
            className={`discovery-wordmark${state === "searching" ? " discovery-wordmark--loading" : ""}`}
            viewBox={`0 0 ${wordmark.width} 120`}
            aria-hidden="true"
            focusable="false"
        >
            <defs>
                {wordmark.letters.map(({ glyph }, index) => (
                    <clipPath id={`${clipId}-${index}`} key={index}>
                        <path d={discoveryGlyphs[glyph]} />
                    </clipPath>
                ))}
            </defs>
            {wordmark.letters.map(({ glyph, x }, index) => (
                <g key={`${index}-${glyph}`} className="discovery-wordmark__letter">
                    <g transform={`translate(${x} 90) scale(.108 -.1)`}>
                        <path d={discoveryGlyphs[glyph]} fill="currentColor" />
                        <g clipPath={`url(#${clipId}-${index})`}>
                            <path
                                d={
                                    index % 2 === 0
                                        ? "M0 730 580 320 580 80 0 490Z"
                                        : "M-60 200 620 610 620 780-60 370Z"
                                }
                                fill="var(--scenery-facet)"
                            />
                            <path
                                d="M80 0 670 300 670 90 80-130Z"
                                fill="var(--scenery-facet-shade)"
                                opacity=".35"
                            />
                            {index === 0 || glyph === "R" || glyph === "M" || glyph === "N" ? (
                                <path d="M300 730 620 530 620 780Z" fill="#e6b56f" />
                            ) : null}
                        </g>
                    </g>
                </g>
            ))}
        </svg>
    );
}
