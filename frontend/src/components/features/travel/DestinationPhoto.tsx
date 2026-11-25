import { useState } from "react";
import { destinationPhotos } from "@/data/destinationPhotos";
import { useTheme } from "@/hooks/useTheme";

export function DestinationPhoto() {
    const { resolved } = useTheme();
    const [selection] = useState(Math.random);
    const photos = destinationPhotos[resolved];
    const photo = photos[Math.floor(selection * photos.length)];
    const path = `/images/destinations/${photo}`;

    return (
        <div className="home-planner__background" aria-hidden="true">
            <picture>
                <source media="(max-aspect-ratio: 3/4)" srcSet={`${path}-mobile.webp`} />
                <img
                    src={`${path}.webp`}
                    srcSet={`${path}.webp 1920w, ${path}-large.webp 3840w`}
                    sizes="100vw"
                    fetchPriority="high"
                    alt=""
                />
            </picture>
        </div>
    );
}
